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
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class DownloadService : Service()
{
    private lateinit var downloadClass : DownloadClass
    private var webSocketService: WebSocketService? = null
    private var bound = false
    private var pending: String? = null
    val client = OkHttpClient.Builder()
//        .addInterceptor { chain ->
//            val newRequest = chain.request().newBuilder()
//                .header("Connection", "close")
//                .build()
//            chain.proceed(newRequest)
//        }
        .connectTimeout(15, TimeUnit.SECONDS)      // conexión inicial
        .readTimeout(30, TimeUnit.MINUTES)        // lectura de bytes grandes
        .writeTimeout(30, TimeUnit.MINUTES)       // si se subiera algo
        .retryOnConnectionFailure(true)           // reintentos automáticos
        .connectionPool(ConnectionPool(
            6,
            30,
            TimeUnit.SECONDS
        ))
        .build()
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            bound = true

            pending?.let {
                startDownload(it)
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(2, notification)

        val baseUrl = intent?.getStringExtra("url")
            ?: return START_NOT_STICKY
        if (bound && webSocketService != null)
        {
            startDownload(baseUrl)
        }
        else
            pending = baseUrl

        return START_NOT_STICKY
    }
    private fun startDownload(baseUrl: String)
    {
        CoroutineScope(Dispatchers.IO).launch {
            downloadClass = DownloadClass(client, webSocketService, this@DownloadService)
            downloadClass.downloadExperience(baseUrl)
        }
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