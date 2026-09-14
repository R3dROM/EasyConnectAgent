package com.easyconnect.agent.utilities

import com.easyconnect.agent.model.JobState
import com.easyconnect.agent.network.report.Report
import java.io.File
import java.security.MessageDigest
object DownloadReport {
    var apkSize: Long = 0L
        private set
    var apkName: String = "null"
        private set
    var status: JobState = JobState.Waiting
        private set
    var bundle: String = "null"
        private set
    var timestamp: Long = 0L
        private set
    var percent: Int = 0
        private set
    var currentFile: String = "null"
        private set
    var jobId: Long = 0
        private set

    fun resetReport(size: Long, name: String, bundle: String, time: Long, percent: Int, currentFile: String, jobId: Long)
    {
        apkSize = size
        apkName = name
        DownloadReport.bundle = bundle
        timestamp = time
        DownloadReport.percent = percent
        DownloadReport.currentFile = currentFile
        DownloadReport.jobId = jobId
    }
    fun updateReport(percent: Int, currentFile: String)
    {
        DownloadReport.percent = percent
        DownloadReport.currentFile = currentFile
    }
    fun updateReport(status: JobState)
    {
        DownloadReport.status = status
    }
    fun endReport(time: Long)
    {
        timestamp = time
    }
}
fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { fis ->
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}