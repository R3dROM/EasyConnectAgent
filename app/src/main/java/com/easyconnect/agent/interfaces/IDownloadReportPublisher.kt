package com.easyconnect.agent.interfaces

import com.easyconnect.agent.deployment.download.DownloadReport

interface IDownloadReportPublisher {
    suspend fun publish(report: DownloadReport)
    suspend fun receive(): DownloadReport
}