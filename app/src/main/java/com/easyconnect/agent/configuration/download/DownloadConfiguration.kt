package com.easyconnect.agent.configuration.download

object DownloadConfiguration
{
    const val CHUNK_SIZE = 32L * 1024 * 1024 //32MB
    const val DOWNLOAD_WORKERS = 8
    const val DOWNLOAD_JOB_ID = "downloadJobId"
    const val DOWNLOAD_CANCELLATION = "downloadCancellation"
    const val DOWNLOAD_URL = "downloadUrl"
    const val BUNDLE = "bundle"
}