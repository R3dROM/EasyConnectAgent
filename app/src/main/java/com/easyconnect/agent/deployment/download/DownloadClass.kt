package com.easyconnect.agent.deployment.download

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.easyconnect.agent.configuration.download.DownloadConfiguration
import com.easyconnect.agent.data.queue.DownloadReportPublisher
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.model.MessageStatus
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.interfaces.IDownloadFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class DownloadClass (
    private var url: String,
    private var jobId: Long,
    private var context: Context
) : IDeployProcess
{
    private var client : OkHttpClient ?= null
    private val dispatcher = Dispatcher().apply {
        maxRequests = DownloadConfiguration.DOWNLOAD_WORKERS
        maxRequestsPerHost = DownloadConfiguration.DOWNLOAD_WORKERS
    }
    private var sendReportJob: Job? = null
    private val serviceScope =
        CoroutineScope(
            Dispatchers.IO + SupervisorJob()
        )

    override suspend fun start(): Boolean {
        if (client == null)
            createClient()
        downloadExperience(url, jobId)

        return true
    }
    private fun createClient()
    {
        client = OkHttpClient.Builder()
            .eventListener(object : EventListener() {

                override fun responseHeadersStart(call: Call) {
                    Log.i("OKHTTP", "responseHeadersStart")
                }

                override fun responseHeadersEnd(call: Call, response: Response) {
                    Log.i("OKHTTP", "responseHeadersEnd ${response.code}")
                }

                override fun responseBodyStart(call: Call) {
                    Log.i("OKHTTP", "responseBodyStart")
                }

                override fun responseBodyEnd(call: Call, byteCount: Long) {
                    Log.i("OKHTTP", "responseBodyEnd $byteCount")
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    Log.e("OKHTTP", "callFailed", ioe)
                }
            })
            .dispatcher(dispatcher)
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.MINUTES)
            .callTimeout(0, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .connectionPool(
                ConnectionPool(
                    DownloadConfiguration.DOWNLOAD_WORKERS,
                    5,
                    TimeUnit.SECONDS
                )
            )
            .build()
    }
    private suspend fun downloadExperience(baseUrl: String?, jobId: Long) {
        val manifestUrl = "$baseUrl/manifest.json"
        client?.let {it ->
            val manifestRaw = downloadManifestRaw(it, manifestUrl)
                ?: return
            val startTime = SystemClock.elapsedRealtime()
            var endTime : Long?
            val manifest = parseManifest(manifestRaw)
            manifest?.let {
                val files = manifest.first
                val bundle = manifest.second
                val outputDir = File(context.getExternalFilesDir(null), bundle)
                val apk = files.firstOrNull{it.pathFile.endsWith(".apk")}
                if (apk != null)
                {
                    val apkName = apk.pathFile.substringAfter("/")
                    DownloadReport.resetReport(
                        apk.size,
                        apkName,
                        MessageStatus.Downloading,
                        bundle,
                        0L,
                        0,
                        "null",
                        jobId
                    )
                }
                try {
                    sendReport(DownloadReport)
                    downloadHelper(files, outputDir, baseUrl)
                    Log.e("DEPLOY", "Descarga completa")
                } catch (e: CancellationException) {
                    Log.e("DOWNLOAD_EXPERIENCE", "Cancellation Exception: $e")
                    DownloadReport.updateReport(MessageStatus.Fail)
                }
                endTime = SystemClock.elapsedRealtime()
                DownloadReport.endReport(endTime - startTime)
            }

        }
    }
    private suspend fun downloadHelper(files: List<IDownloadFiles>, outputDir: File, baseUrl: String?)
    {
        files.let {
            for (file in it) {
                currentCoroutineContext().ensureActive()
                val finalFile = File(outputDir, file.pathFile)
                if (finalFile.exists() && finalFile.length() == file.size)
                {
                    val hash = sha256(finalFile)
                    if (hash == file.sha256)
                    {
                        Log.e("DEPLOY", "El archivo ya existe, saltando: ${file.pathFile}")
                        continue // No descargar
                    }
                    else
                    {
                        finalFile.delete()
                        DownloadReport.updateReport(MessageStatus.Fail)
                        Log.e("DEPLOY","${file.pathFile} está corrupto o ha cambiado, volviendo a descargar")
                    }
                }
                DownloadReport.updateReport(MessageStatus.Downloading)
                Log.e("DEPLOY", "Descargando ${file.pathFile}")
                downloadFile(baseUrl, file, outputDir, file.size)
            }
        }
    }
    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun downloadFile(
        baseUrl: String?,
        file: IDownloadFiles,
        outputDir: File,
        size: Long,
    ) {
        withContext(Dispatchers.IO)
        {
            coroutineContext.ensureActive()
            val queue = splitChunks(size)
            val queueLock = Any()
            val downloadedBytes = AtomicLong(0)
            val lastUpdate = AtomicLong(0)
            val lock = Any()
            val outputFile =
                File(
                outputDir,
                when(file.pathFile)
                {
                    "CONFIGS/${PersistentData.agentConfigurationReader.serialNumber}.json" -> "files/NetworkingConfiguration.json"
                    else -> file.pathFile
                }
            )
            outputFile.parentFile?.mkdirs()

            RandomAccessFile(outputFile, "rw").use { raf ->
                raf.setLength(size)
                val channel = raf.channel

                val jobs = List(DownloadConfiguration.DOWNLOAD_WORKERS) {

                    async(Dispatchers.IO) {
                        downloadWorker(
                            client!!,
                            queue,
                            queueLock,
                            "${baseUrl}/${file.pathFile}",
                            channel,
                            file,
                            downloadedBytes,
                            size,
                            lastUpdate,
                            lock
                        ) { percent ->
                            DownloadReport.updateReport(percent, "(${outputFile.name}")
                        }
                    }
                }

                jobs.awaitAll()
                raf.channel.force(true)
                outputFile.setReadable(true, false)
                outputFile.setWritable(true, false)
                outputFile.setExecutable(true, false)
            }
            DownloadReport.updateReport(100, "(${outputFile.name}")
        }
    }
    override fun shutdown() {
        dispatcher.cancelAll()
        client?.dispatcher?.cancelAll()
        sendReportJob?.cancel()
        serviceScope.cancel()
    }

    override suspend fun sendReport(report: DownloadReport) {
        sendReportJob = serviceScope.launch {
            while (isActive)
            {
                DownloadReportPublisher.publish(report)
                if (report.timestamp > 0)
                    break
                delay(1000.milliseconds)
            }
        }
    }
}