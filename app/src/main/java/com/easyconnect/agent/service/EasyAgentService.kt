package com.easyconnect.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.easyconnect.agent.configuration.agent.AgentConfiguration
import com.easyconnect.agent.configuration.download.DownloadConfiguration
import com.easyconnect.agent.core.EasyAgentClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EasyAgentService : Service()
{
    private var easyAgentClass : EasyAgentClass ?= null
    private var agentJob : Job?= null
    private var deploymentJob : Job ?= null
    private var experienceJob: Job ?= null
    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val notification = createNotification()
        startForeground(1, notification)
        easyAgentClass = EasyAgentClass(this)
        agentJob = serviceScope.launch {
            val result = easyAgentClass?.bootAgent()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intentService = intent?.getStringExtra(AgentConfiguration.TARGET_SERVICE)
        Log.i("INTENT", "receive: $intentService")
        when(intentService)
        {
            AgentConfiguration.STOP_DEPLOYMENT_SERVICE ->
            {
                val stopDeployment = intent.getBooleanExtra(DownloadConfiguration.DOWNLOAD_CANCELLATION, false)
                if (stopDeployment)
                {
                    reset()
                    return START_STICKY
                }
            }

            AgentConfiguration.START_DEPLOYMENT_SERVICE -> {
                Log.i("INTENT", "starting: $intentService")

                deploymentJob = serviceScope.launch {
                    easyAgentClass?.startDeploymentService(intent)
                }
            }
            AgentConfiguration.START_EXPERIENCE ->
            {
                experienceJob = serviceScope.launch {
                    easyAgentClass?.startExperience(intent)
                }
            }
            AgentConfiguration.START_PICO_CONFIGURATION -> {}
        }

        return START_STICKY
    }
    override fun onBind(intent: Intent?) = null
    private fun createNotification(): Notification {
        val channelId = "EasyAgent_Channel"

        val channel = NotificationChannel(
            channelId,
            "EASY AGENT",
            NotificationManager.IMPORTANCE_HIGH
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("EasyAgent")
            .setContentText("EasyAgentService")
            .build()
    }
    override fun onDestroy() {
        reset()
        easyAgentClass?.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }
    private fun reset()
    {
        agentJob?.cancel()
        deploymentJob?.cancel()
        experienceJob?.cancel()
        easyAgentClass?.shutDown()
    }
}