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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class DownloadService : Service()
{
    private var webSocketService: WebSocketService? = null
    private var bound = false
    private var pending: String? = null
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)      // conexión inicial
        .readTimeout(30, TimeUnit.MINUTES)        // lectura de bytes grandes
        .writeTimeout(30, TimeUnit.MINUTES)       // si se subiera algo
        .retryOnConnectionFailure(true)           // reintentos automáticos
        .build()
    private lateinit var downloadClass : DownloadClass
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            bound = true

            pending?.let {
                StartDownload(it)
                pending = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            webSocketService = null
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
            unbindService(connection)
        super.onDestroy()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(2, notification)

        val baseUrl = intent?.getStringExtra("url")
            ?: return START_NOT_STICKY
        if (bound && webSocketService != null)
            StartDownload(baseUrl)
        else
            pending = baseUrl

        return START_NOT_STICKY
    }
    private fun StartDownload(baseUrl: String)
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
    override fun onBind(intent: Intent?) = null
}