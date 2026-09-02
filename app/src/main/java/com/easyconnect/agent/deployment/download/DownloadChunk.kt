package com.easyconnect.agent.deployment.download

import android.util.Log
import com.easyconnect.agent.configuration.download.DownloadConfiguration
import com.easyconnect.agent.interfaces.IDownloadFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException

data class Chunk(
    val start: Long,
    val end: Long
)

fun splitChunks(size: Long): ArrayDeque<Chunk> {
    val queue = ArrayDeque<Chunk>()

    var start = 0L

    while (start < size) {
        val end = minOf(start + DownloadConfiguration.CHUNK_SIZE - 1, size - 1)
        queue.addLast(Chunk(start, end))
        start = end + 1
    }
    return queue
}
@OptIn(ExperimentalAtomicApi::class)
suspend fun downloadChunk(
    client: OkHttpClient,
    url: String,
    chunk: Chunk,
    channel: FileChannel,
    file: IDownloadFiles,
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
        coroutineContext.ensureActive()
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=${chunk.start}-${chunk.end}")
                .build()
            Log.d("DOWNLOAD", "Solicitando ${chunk.start}-${chunk.end}")
            val call = client.newCall(request)
            coroutineContext.job.invokeOnCompletion {
                call.cancel()
            }
            call.execute().use { response ->
                Log.d("DOWNLOAD", "Respuesta recibida ${response.code}")
                response.isSuccessful.let {
                    if (!it) {
                        Log.e("DEPLOY", "Error descargando ${file.pathFile}: ${response.code}")
                    }
                }
                if (response.code != 206) {
                    throw Exception("Server no soporta partial content")
                }

                response.body.byteStream().use { input ->
                    val buffer = ByteArray(1024 * 256)
                    Log.d("DOWNLOAD", "Esperando datos...")
                    while (true) {
                        coroutineContext.ensureActive()
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
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            Log.e("DOWNLOAD", "Chunk download failed", e)
            retries--
            if (retries == 0) throw e
        }
    }
}