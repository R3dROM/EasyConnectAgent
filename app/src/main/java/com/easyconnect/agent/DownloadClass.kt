package com.easyconnect.agent

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val CHUNK_SIZE = 32L * 1024 * 1024 //32MB
const val DOWNLOAD_WORKERS = 4
data class Chunk(val start: Long, val end: Long)
data class ManifestFile(
    val path: String,
    val size: Long,
    val sha256: String
)
data class DownloadReport(
    val apkSize: Long = 0L,
    val apkName: String = "null",
    var status: String = "null",
    var bundle: String = "null",
    var timestamp: Long = 0L,
    var percent: Int = 0,
    var currentFile: String = "null"
)
class DownloadClass (
    private val client: OkHttpClient,
    private val webSocketService: WebSocketService?,
    private val context: Context
)
{
    fun splitChunks(size: Long): ArrayDeque<Chunk> {
        val queue = ArrayDeque<Chunk>()

        var start = 0L

        while (start < size) {
            val end = minOf(start + CHUNK_SIZE - 1, size - 1)
            queue.addLast(Chunk(start, end))
            start = end + 1
        }

        return queue
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
    private suspend fun downloadManifestRaw(manifestUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(manifestUrl)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    response.body.string()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    private fun parseManifest(jsonString: String): Pair<List<ManifestFile>, String> {

        val filesList = mutableListOf<ManifestFile>()

        val jsonObject = JSONObject(jsonString)
        val filesArray = jsonObject.getJSONArray("files")
        val netConfigsArray = jsonObject.getJSONArray("netConfigs")
        val bundle = jsonObject.getString("bundle")

        for (i in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(i)

            val path = item.getString("path")
            val size = item.getLong("size")
            val sha256 = item.getString("sha256")

            filesList.add(
                ManifestFile(path, size, sha256)
            )
        }
        for (i in 0 until netConfigsArray.length())
        {
            val item = netConfigsArray.getJSONObject(i)

            val path = item.getString("path")
            val sha256 = item.getString("sha256")
            val size = item.getLong("size")
            val serialNumber = item.getString("serialNumber")
            if (serialNumber == webSocketService?.getSerialNumber())
            {
                filesList.add(
                    ManifestFile(path, size, sha256)
                )
                return Pair(filesList, bundle)
            }
        }
        return Pair(filesList, bundle)
    }
    suspend fun downloadExperience(baseUrl: String) {
        lateinit var downloadReport : DownloadReport
        val manifestUrl = "$baseUrl/manifest.json"
        val manifestRaw = downloadManifestRaw(manifestUrl)
            ?: return

        val start = android.os.SystemClock.elapsedRealtime()
        val manifest = parseManifest(manifestRaw)
        val files = manifest.first
        val bundle = manifest.second
        val outputDir = File(context.getExternalFilesDir(null), bundle)
        val apk = files.firstOrNull{it.path.endsWith(".apk")}
        if (apk != null)
        {
            val apkName = apk.path.substringAfter("/")
            downloadReport = DownloadReport(
                apkSize = apk.size,
                apkName = apkName,
                status = "Downloading",
                bundle = bundle,
            )
        }
        webSocketService?.sendDownloadStatus(downloadReport)
        for (file in files) {
            val finalFile = File(outputDir, file.path)
            if (finalFile.exists() && finalFile.length() == file.size)
            {
                val hash = sha256(finalFile)
                if (hash == file.sha256)
                {
                    Log.e("DEPLOY", "El archivo ya existe, saltando: ${file.path}")
                    continue // No descargar
                }
                else
                {
                    finalFile.delete()
                    Log.e("DEPLOY","${file.path} está corrupto o ha cambiado, volviendo a descargar")
                }
            }
            Log.e("DEPLOY", "Descargando ${file.path}")
            downloadReport.currentFile = "(${files.indexOf(file) + 1}/${files.size}) - ${finalFile.name}"
            downloadFile(baseUrl, file, outputDir, file.size, downloadReport)
        }
        Log.e("DEPLOY", "Descarga completa")
        val end = android.os.SystemClock.elapsedRealtime()
        downloadReport.status = "Download Complete"
        downloadReport.timestamp = end - start
    }
    @OptIn(ExperimentalAtomicApi::class)
    suspend fun downloadChunk(
        url: String,
        chunk: Chunk,
        channel: FileChannel,
        file: ManifestFile,
        downloadedBytes: AtomicLong,
        totalSize: Long,
        lastUpdate: AtomicLong,
        lock: Any,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {

        var retries = 3
        var success = false
        var position = chunk.start

        while (!success && retries > 0)
        {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=${chunk.start}-${chunk.end}")
                    .build()
                Log.d("DOWNLOAD", "Solicitando ${chunk.start}-${chunk.end}")
                client.newCall(request).execute().use { response ->
                    Log.d("DOWNLOAD", "Respuesta recibida ${response.code}")
                    if (!response.isSuccessful) {
                        Log.e("DEPLOY", "Error descargando ${file.path}: ${response.code}")
                    }
                    if (response.code != 206) {
                        throw Exception("Server no soporta partial content")
                    }

                    response.body.byteStream().use { input ->
                        val buffer = ByteArray(1024 * 256)
                        Log.d("DOWNLOAD", "Esperando datos...")
                        while (true) {
                            val read = input.read(buffer)
                            Log.d("DOWNLOAD", "Leídos: $read")
                            if (read == -1)
                            {
                                success = true
                                break
                            }

                            val byteBuffer = ByteBuffer.wrap(buffer, 0, read)
                            synchronized(lock)
                            {
                                channel.write(byteBuffer, position)
                            }

                            position += read

                            val totalDownloaded = downloadedBytes.addAndFetch(read.toLong())
                            val percent = ((totalDownloaded * 100) / totalSize).toInt()

                            val now = System.currentTimeMillis()
                            val last = lastUpdate.load()
                            if (now - last > 200 && lastUpdate.compareAndSet(last, now)) {
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            catch (e: Exception) {
                Log.e("DOWNLOAD", "Chunk download failed", e)
                retries--
                if (retries == 0) throw e
            }
        }
    }
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun downloadWorker(
        queue: ArrayDeque<Chunk>,
        queueLock: Any,
        url: String,
        channel: FileChannel,
        file: ManifestFile,
        downloadedBytes: AtomicLong,
        totalSize: Long,
        lastUpdate: AtomicLong,
        writeLock: Any,
        onProgress: (Int) -> Unit
    ) {
        while (true) {

            val chunk = synchronized(queueLock) {
                if (queue.isEmpty()) null
                else queue.removeFirst()
            } ?: break

            downloadChunk(
                url,
                chunk,
                channel,
                file,
                downloadedBytes,
                totalSize,
                lastUpdate,
                writeLock,
                onProgress
            )
        }
    }
    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    @OptIn(ExperimentalAtomicApi::class)
    private suspend fun downloadFile(
        baseUrl: String,
        file: ManifestFile,
        outputDir: File,
        size: Long,
        downloadReport: DownloadReport
    ) {
        withContext(Dispatchers.IO) {
            val queue = splitChunks(size)
            val queueLock = Any()
            val downloadedBytes = AtomicLong(0)
            val lastUpdate = AtomicLong(0)
            val lock = Any()
            val outputFile = if (file.path == "CONFIGS/${webSocketService?.getSerialNumber()}.json") {
                File(outputDir, "CONFIGS/NetworkingConfiguration.json")
            } else {
                File(outputDir, file.path)
            }
            outputFile.parentFile?.mkdirs()

            RandomAccessFile(outputFile, "rw").use { raf ->
                raf.setLength(size)
                val channel = raf.channel

                val jobs = List(DOWNLOAD_WORKERS) {

                    async(Dispatchers.IO) {

                        downloadWorker(
                            queue,
                            queueLock,
                            "${baseUrl}/${file.path}",
                            channel,
                            file,
                            downloadedBytes,
                            size,
                            lastUpdate,
                            lock
                        ) { percent ->
                            downloadReport.percent = percent
                        }
                    }
                }

                jobs.awaitAll()
                raf.channel.force(true)
                outputFile.setReadable(true, false)
                outputFile.setWritable(true, false)
            }
            downloadReport.percent = 100
        }
    }
}