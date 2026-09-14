package com.easyconnect.agent.deployment

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.data.queue.Communicator
import com.easyconnect.agent.dependency.AgentDependencies
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.model.DeploymentState
import com.easyconnect.agent.model.JobState
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.network.report.Report
import com.easyconnect.agent.utilities.DownloadReport
import com.easyconnect.agent.utilities.JsonBuilder
import com.flyfishxu.kadb.Kadb
class DeploymentClass(
    extras: Bundle?,
    private val jobId: Long,
    private var appContext: Context
) : IDeployProcess{
    data class Process(
        var state: DeploymentState,
        var execute: suspend() -> Boolean
    )
    private var url: String ?= null
    private var bundle: String ?= null
    private var installApk : IDeployProcess ?= null
    private var moveFiles : IDeployProcess ?= null
    private var downloadClass : IDeployProcess ?= null
    private var adbClient : Kadb ?= null
    private val deploymentStages = mutableListOf<Process>()
    private var currentState: DeploymentState ?= null
    private var timestamp: Long ?= null

    private val startTime = SystemClock.elapsedRealtime()

    init
    {
        adbClient = Kadb.create("127.0.0.1",5555).use { kadb ->
            val response = kadb.shell("echo hello")
            check(response.exitCode == 0)
            check(response.output == "hello\n")
            kadb
        }

        url = extras?.getString(ActivityConfiguration.URL) ?: ""
        bundle = extras?.getString(ActivityConfiguration.BUNDLE) ?: ""

        deploymentStages.add(
            Process(
                state = DeploymentState.Download
            ) {
                startDownloadProcess()
            }
        )
        deploymentStages.add(
            Process(
                state = DeploymentState.Move
            ) {
                startMoveFilesProcess()
            }
        )
        deploymentStages.add(
            Process(
                state = DeploymentState.Install
            ) {
                startInstallProcess()
            }
        )
    }
    suspend fun startDownloadProcess(): Boolean
    {
        if (downloadClass == null)
            downloadClass = AgentDependencies.createDownloadClass(
                url = url ?: "null",
                jobId = jobId ?: -1,
                appContext = appContext
            )
        Log.i("DEPLOYMENT_SERVICE", "DOWNLOAD PROCESS CREATED")
        val downloadResponse = downloadClass?.start()
        if (downloadResponse != null)
            return downloadResponse
        return false
    }
    suspend fun startMoveFilesProcess(): Boolean
    {
        moveFiles = AgentDependencies.createMoveFilesClass(
            appContext = appContext,
            adbClient = adbClient,
            bundle = bundle ?: "null"
            )
        Log.i("DEPLOYMENT_SERVICE", "MOVE FILES PROCESS CREATED")
        val moveResponse = moveFiles?.start()
        if (moveResponse != null)
            return moveResponse
        return false
    }
    suspend fun startInstallProcess(): Boolean
    {
        installApk = AgentDependencies.createInstallClass(
            adbClient = adbClient,
        )
        Log.i("DEPLOYMENT_SERVICE", "INSTALL PROCESS CREATED")
        val installResponse = installApk?.start()
        if (installResponse != null)
            return installResponse
        return false
    }

    override suspend fun start(): Boolean {
        Log.i("DEPLOYMENT_SERVICE", "CREATING DEPLOYMENT PROCESS")
        var deployProcess = false
        deploymentStages.forEach {
            deployProcess = it.execute.invoke()
            if (!deployProcess)
                return@forEach
        }
        currentState = if (deployProcess)
            DeploymentState.Complete
        else
            DeploymentState.Fail

        sendReport()
        shutdown()
        return true
    }

    override fun shutdown() {
        downloadClass?.shutdown()
        moveFiles?.shutdown()
        installApk?.shutdown()
        adbClient?.close()
    }

    override suspend fun sendReport() {
        val endtime = SystemClock.elapsedRealtime()
        timestamp = endtime - startTime

        val payload = JsonBuilder.putExtras(
            JsonBuilder.extra("status", currentState)
        )

        val report = Report(
            id = PersistentData.agentConfigurationReader.serialNumber,
            payload = payload,
            type = MessageType.Acknowledge,
            timestamp = timestamp,
            jobId = jobId
        )
        Communicator.publishReport(report)
    }
}