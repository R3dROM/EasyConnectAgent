package com.easyconnect.agent.deployment.download

import com.easyconnect.agent.interfaces.IDownloadFiles
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import java.nio.channels.FileChannel
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
suspend fun downloadWorker(
    client: OkHttpClient,
    queue: ArrayDeque<Chunk>,
    queueLock: Any,
    url: String,
    channel: FileChannel,
    file: IDownloadFiles,
    downloadedBytes: AtomicLong,
    totalSize: Long,
    lastUpdate: AtomicLong,
    writeLock: Any,
    onProgress: (Int) -> Unit
) {
    while (true) {
        currentCoroutineContext().ensureActive()
        val chunk = synchronized(queueLock) {
            if (queue.isEmpty()) null
            else queue.removeFirst()
        } ?: break
        downloadChunk(
            client,
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