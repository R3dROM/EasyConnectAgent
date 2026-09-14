package com.easyconnect.agent.deployment.download

import android.util.Log
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.interfaces.IDownloadFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONObject
import java.net.SocketTimeoutException

data class ManifestFile(
    override val pathFile: String,
    override val size: Long,
    override val sha256: String,
) : IDownloadFiles
suspend fun downloadManifestRaw(client: OkHttpClient, manifestUrl: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(manifestUrl)
                .build()
            val call = client.newCall(request)
            coroutineContext.job.invokeOnCompletion {
                call.cancel()
            }
            call.execute().use { response ->
                response.isSuccessful.let { if (!it) return@withContext null }
                response.body.string()
            }
        } catch (e: Exception) {
            Log.e("DOWNLOAD_MANIFEST", "Exception: $e")
            e.printStackTrace()
        } catch (e: SocketTimeoutException)
        {
            Log.e("DOWNLOAD_MANIFEST", "Socket Timeout Exception: $e")
            e.printStackTrace()
        } catch (e: IOException)
        {
            Log.e("DOWNLOAD_MANIFEST", "IO Exception: $e")
            e.printStackTrace()
        } as String?
    }
}
fun parseManifest(jsonString: String): Pair<List<ManifestFile>, String>?
{
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

        val serialNumber = item.getString("serialNumber")
        if (serialNumber == PersistentData.agentConfigurationReader.serialNumber)
        {
            val path = item.getString("path")
            val sha256 = item.getString("sha256")
            val size = item.getLong("size")

            filesList.add(
                ManifestFile(path, size, sha256)
            )
            return Pair(filesList, bundle)
        }
    }
    return null
}