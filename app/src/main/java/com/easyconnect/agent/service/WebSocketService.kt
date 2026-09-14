package com.easyconnect.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.configuration.agent.AgentConfiguration
import com.easyconnect.agent.dependency.AgentDependencies
import com.easyconnect.agent.interfaces.IInterpreter
import com.easyconnect.agent.interfaces.IWebSocket
import com.easyconnect.agent.network.interpreter.Interpreter

class WebSocketService : Service()
{
    var webSocketClass: IWebSocket ?= null
    var interpreter: IInterpreter = Interpreter

    override fun onCreate() {
        val notification = createNotification()
        startForeground(2, notification)
        super.onCreate()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        Log.i("WEBSOCKET", "Intent receive: $intent")
        val bundle = intent?.getBundleExtra(ActivityConfiguration.EXTRAS)
        val websocketUrl =
            bundle?.getString(ActivityConfiguration.URL)
        val webSocketPort =
            bundle?.getString(ActivityConfiguration.PORT)
        val registerJobId =
            bundle?.getLong(ActivityConfiguration.JOB_ID, 0)
        val stopWebSocket =
            bundle?.getBoolean(ActivityConfiguration.CANCELLATION, false) ?: false

        if (stopWebSocket)
        {
            webSocketClass?.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }
        if (websocketUrl != null)
        {
            if (webSocketClass == null)
            {
                val pendingUrl = "ws://${websocketUrl}:${webSocketPort}"
                webSocketClass = AgentDependencies.createWebSocketClass(
                    url = pendingUrl,
                    jobId = registerJobId ?: 0,
                    interpreter,
                    appContext = this
                )
            }
            webSocketClass?.isConnected()?.let {
                if (!it) {
                    webSocketClass?.startWebSocketClient()
                }
            }
        }
        return START_NOT_STICKY
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