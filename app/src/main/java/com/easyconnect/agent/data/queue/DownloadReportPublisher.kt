package com.easyconnect.agent.data.queue

import com.easyconnect.agent.deployment.download.DownloadReport
import com.easyconnect.agent.interfaces.IDownloadReportPublisher
import kotlinx.coroutines.channels.Channel

object DownloadReportPublisher : IDownloadReportPublisher {
    val queue = Channel<DownloadReport>(
        Channel.UNLIMITED
    )
    override suspend fun publish(report: DownloadReport)
    {
        queue.send(report)
    }
    override suspend fun receive() : DownloadReport{
        return queue.receive()
    }
}