package com.easyconnect.agent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebSocketService : Service()
{
    private lateinit var webSocketClass: WebSocketClass
    inner class LocalBinder : Binder() {
        fun getService() : WebSocketService = this@WebSocketService
    }
    private val binder = LocalBinder()
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)      // conexión inicial
        .readTimeout(30, TimeUnit.MINUTES)        // lectura de bytes grandes
        .writeTimeout(30, TimeUnit.MINUTES)       // si se subiera algo
        .retryOnConnectionFailure(true)           // reintentos automáticos
        .build()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        val websocketUrl = intent?.getStringExtra("webSocketUrl")
            ?: return START_STICKY
        if (!::webSocketClass.isInitialized) {
            webSocketClass = WebSocketClass(client, websocketUrl, this)
            webSocketClass.connect()
        }
        return START_STICKY
    }
    override fun onDestroy() {
        if (::webSocketClass.isInitialized)
            webSocketClass.disconnect()
        super.onDestroy()
    }
    fun sendMessage(message: String)
    {
        if (::webSocketClass.isInitialized)
            webSocketClass.sendMessage(message)
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
    override fun onBind(intent: Intent?): IBinder = binder
}