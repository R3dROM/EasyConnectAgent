package com.easyconnect.agent.deployment

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import com.easyconnect.agent.configuration.download.DownloadConfiguration
import com.easyconnect.agent.data.queue.DownloadReportPublisher
import com.easyconnect.agent.dependency.AgentDependencies
import com.easyconnect.agent.deployment.download.DownloadReport
import com.easyconnect.agent.deployment.install.InstallApk
import com.easyconnect.agent.deployment.movefiles.MoveFilesClass
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.interfaces.IPicoControlAPP
import com.easyconnect.agent.interfaces.IPicoFile
import com.easyconnect.agent.model.DeploymentState
import com.easyconnect.agent.model.MessageStatus
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DeploymentClass(
    private var url: String,
    private var bundle: String,
    private var jobId: Long,
    private var appContext: Context
) : IDeployProcess{
    class Process(
        var state: DeploymentState,
        var execute: suspend() -> Unit
    )
    private var installApk : IDeployProcess ?= null
    private var moveFiles : IDeployProcess ?= null
    private var downloadClass : IDeployProcess ?= null
    private var adbClient : Kadb ?= null
    private val deploymentStages = mutableListOf<Process>()
    private fun createProcess()
    {
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
    suspend fun startDownloadProcess()
    {
        DownloadReport.updateReport(MessageStatus.Downloading)
        DownloadReportPublisher.publish(DownloadReport)
        if (downloadClass == null)
            downloadClass = AgentDependencies.createDownloadClass(
                url = url,
                jobId = jobId,
                appContext = appContext
            )
        Log.i("DEPLOYMENT_SERVICE", "DOWNLOAD PROCESS CREATED")
        downloadClass?.start()
    }
    suspend fun startMoveFilesProcess()
    {
        DownloadReport.updateReport(MessageStatus.Moving)
        DownloadReportPublisher.publish(DownloadReport)
        moveFiles = AgentDependencies.createMoveFilesClass(
            appContext = appContext,
            adbClient = adbClient,
            bundle = bundle
            )
        Log.i("DEPLOYMENT_SERVICE", "MOVE FILES PROCESS CREATED")
        moveFiles?.start()
    }
    suspend fun startInstallProcess()
    {
        DownloadReport.updateReport(MessageStatus.Installing)
        DownloadReportPublisher.publish(DownloadReport)
        installApk = AgentDependencies.createInstallClass(
            appContext = appContext,
            adbClient = adbClient,
        )
        Log.i("DEPLOYMENT_SERVICE", "INSTALL PROCESS CREATED")
        installApk?.start()
    }

    override suspend fun start(): Boolean {
        adbClient = Kadb.create("127.0.0.1",5555).use { kadb ->
            val response = kadb.shell("echo hello")
            check(response.exitCode == 0)
            check(response.output == "hello\n")
            kadb
        }
        Log.i("DEPLOYMENT_SERVICE", "CREATING DEPLOYMENT PROCESS")
        createProcess()
        deploymentStages.forEach { it ->
            val result = it.execute.invoke()
        }
        DownloadReport.updateReport(MessageStatus.Complete)
        DownloadReportPublisher.publish(DownloadReport)
        shutdown()
        return true
    }

    override fun shutdown() {
        downloadClass?.shutdown()
        moveFiles?.shutdown()
        installApk?.shutdown()
        adbClient?.close()
    }

    override suspend fun sendReport(report: DownloadReport) {

    }
}