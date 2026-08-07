package com.easyconnect.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadService : Service()
{
    private lateinit var downloadClass : DownloadClass
    private var webSocketService: WebSocketService? = null
    private var bound = false
    private var pending: String? = null
    private var downloadJobId: Long = 0
    private var job: Job? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    val dispatcher = Dispatcher().apply {
        maxRequests = DOWNLOAD_WORKERS
        maxRequestsPerHost = DOWNLOAD_WORKERS
    }
    val client = OkHttpClient.Builder()
        .eventListener(object : EventListener() {

            override fun responseHeadersStart(call: Call) {
                Log.d("OKHTTP", "responseHeadersStart")
            }

            override fun responseHeadersEnd(call: Call, response: Response) {
                Log.d("OKHTTP", "responseHeadersEnd ${response.code}")
            }

            override fun responseBodyStart(call: Call) {
                Log.d("OKHTTP", "responseBodyStart")
            }

            override fun responseBodyEnd(call: Call, byteCount: Long) {
                Log.d("OKHTTP", "responseBodyEnd $byteCount")
            }

            override fun callFailed(call: Call, ioe: IOException) {
                Log.e("OKHTTP", "callFailed", ioe)
            }
        })
        .dispatcher(dispatcher)
        .connectTimeout(1, TimeUnit.MINUTES)      // conexión inicial
        .readTimeout(2, TimeUnit.MINUTES)        // lectura de bytes grandes
        .callTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.MINUTES)       // si se subiera algo
        .retryOnConnectionFailure(true)           // reintentos automáticos
        .connectionPool(ConnectionPool(
            DOWNLOAD_WORKERS,
            5,
            TimeUnit.SECONDS
        ))
        .build()
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            bound = true

            pending?.let {
                startDownload(it, downloadJobId)
                pending = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cleanEverything()
        }
    }
    override fun onCreate() {
        super.onCreate()

        val startIntent = Intent(this, WebSocketService::class.java)
        startForegroundService(startIntent)

        bindService(startIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (bound)
        {
            cleanEverything()
        }
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(2, notification)

        downloadJobId = intent?.getLongExtra("downloadJobId", 0) ?: 0
        val cancel = intent?.getBooleanExtra("cancel", false) ?: false
        if (cancel)
        {
            cancelDownload()
            return  START_NOT_STICKY
        }
        val baseUrl = intent?.getStringExtra("url")
            ?: return START_NOT_STICKY
        if (bound && webSocketService != null)
        {
            startDownload(baseUrl, downloadJobId)
        }
        else
            pending = baseUrl

        return START_NOT_STICKY
    }
    private fun startDownload(baseUrl: String, jobId: Long)
    {
        job = serviceScope.launch {
            downloadClass = DownloadClass(client, webSocketService,this@DownloadService)
            downloadClass.downloadExperience(baseUrl, jobId)
        }
    }
    private fun cancelDownload()
    {
        job?.cancel()
        dispatcher.cancelAll()
    }
    private fun createNotification(): Notification {
        val channelId = "deploy_channel"

        val channel = NotificationChannel(
            channelId,
            "DEPLOY",
            NotificationManager.IMPORTANCE_HIGH
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("EasyDeploy")
            .setContentText("Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
    }
    private fun cleanEverything()
    {
        unbindService(connection)
        bound = false
        webSocketService = null
        pending = null
        stopSelf()
    }
    override fun onBind(intent: Intent?) = null
}