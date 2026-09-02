package com.easyconnect.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.easyconnect.agent.configuration.network.NetworkConfiguration
import com.easyconnect.agent.dependency.AgentDependencies
import com.easyconnect.agent.interfaces.IWebSocket

class WebSocketService : Service()
{
    var webSocketClass: IWebSocket ?= null

    override fun onCreate() {
        val notification = createNotification()
        startForeground(2, notification)
        super.onCreate()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val websocketUrl =
            intent?.getStringExtra(NetworkConfiguration.WEBSOCKET_URL)
        val webSocketPort =
            intent?.getStringExtra(NetworkConfiguration.WEBSOCKET_PORT)
        val registerJobId =
            intent?.getLongExtra(NetworkConfiguration.REGISTER_JOB_ID, 0)
        val stopWebSocket =
            intent?.getBooleanExtra(NetworkConfiguration.WEBSOCKET_STOP, false) ?: false

        if (stopWebSocket)
        {
            webSocketClass?.disconnect()
            stopSelf()
            return START_STICKY
        }
        if (websocketUrl != null)
        {
            if (webSocketClass == null)
            {
                val pendingUrl = "ws://${websocketUrl}:${webSocketPort}"
                webSocketClass = AgentDependencies.createWebSocketClass(
                    url = pendingUrl,
                    jobId = registerJobId ?: 0,
                    appContext = this
                )
            }
            webSocketClass?.isConnected()?.let {
                if (!it) {
                    webSocketClass?.startWebSocketClient()
                }
            }
        }
        return START_STICKY
    }
    override fun onDestroy() {
        webSocketClass?.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    private fun createNotification(): Notification {
        val channelId = "communication_channel"

        val channel = NotificationChannel(
            channelId,
            "COMMUNICATION",
            NotificationManager.IMPORTANCE_HIGH
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("EasyAgent")
            .setContentText("Communicating...")
            .build()
    }
    override fun onBind(intent: Intent?) = null
}