package com.easyconnect.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.core.EasyAgentClass
import com.easyconnect.agent.model.JobType
import com.easyconnect.agent.utilities.DownloadReport
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(
                ActivityConfiguration.TARGET,
                JobType::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(
                ActivityConfiguration.TARGET
            ) as? JobType?
        }
        val jobId = intent?.getLongExtra(ActivityConfiguration.JOB_ID, -1) ?: -1
        val extras = intent?.getBundleExtra(ActivityConfiguration.EXTRAS)
        val options = intent?.getBundleExtra(ActivityConfiguration.OPTIONS)
        val cancellation = extras?.getBoolean(
            ActivityConfiguration.CANCELLATION,
            false
        ) ?: false
        Log.i("INTENT", "receive: $options & $extras" )
        when
        {
            command == JobType.Cancellation -> {
                if (cancellation) {
                    Log.i("DEPLOYMENT SERVICE", "Cancelling deployment")

                    deploymentJob?.cancel()
                    easyAgentClass?.shutDown()

                    deploymentJob = null

                    return START_STICKY
                }
                deploymentJob = serviceScope.launch {
                    val result = easyAgentClass?.startDeploymentService(jobId, extras, options)
                    if (result == false)
                      deploymentJob?.cancel()
                    Log.i("DEPLOYMENT SERVICE", "DEPLOYMENT: $result")
                }
            }
            command == JobType.Deployment -> {
                deploymentJob = serviceScope.launch {
                    val result = easyAgentClass?.startDeploymentService(jobId, extras, options)
                    if (result == false)
                        deploymentJob?.cancel()
                    Log.i("DEPLOYMENT SERVICE", "DEPLOYMENT: $result")
                }
            }
            command == JobType.StartExperience -> {
                deploymentJob = serviceScope.launch {
                    val appName = extras?.getString("bundle") ?: DownloadReport.bundle
                    val result = easyAgentClass?.startExperience(appName, jobId)
                }
            }
            command == JobType.UninstallExperience -> {
                deploymentJob = serviceScope.launch {
                    val appName = extras?.getString("bundle") ?: DownloadReport.bundle
                    val result = easyAgentClass?.uninstallExperience(appName, jobId)
                }
            }
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