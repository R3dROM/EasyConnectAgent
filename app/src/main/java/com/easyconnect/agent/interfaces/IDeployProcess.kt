package com.easyconnect.agent.interfaces

import android.content.Context
import com.easyconnect.agent.deployment.download.DownloadReport
interface IDeployProcess {
    suspend fun start() : Boolean
    fun shutdown()
    suspend fun sendReport(report: DownloadReport)
}