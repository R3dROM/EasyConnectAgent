package com.easyconnect.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class ManifestFile(
    val path: String,
    val size: Long,
    val sha256: String
)
data class DeviceReport(
    val deviceIp: String? = "null",
    val apkPath: String = "null",
    val apkSize: Long = 0L,
    val apkName: String = "null",
    var status: Boolean = false,
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

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                response.body.string()

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
        return Pair(filesList, bundle)
    }
    suspend fun downloadExperience(baseUrl: String) {

        lateinit var deviceReport : DeviceReport
        val manifestUrl = "$baseUrl/manifest.json"
        val manifestRaw = downloadManifestRaw(manifestUrl)
            ?: return

        val manifest = parseManifest(manifestRaw)
        val files = manifest.first
        val bundle = manifest.second
        val outputDir = File(context.getExternalFilesDir(null), bundle)
        val apk = files.firstOrNull{it.path.endsWith(".apk")} // -> Only one file can be the apk
        if (apk != null)
        {
            val apkPath = File(outputDir, apk.path)
            val apkName = apk.path.substringAfter("/")
            deviceReport = DeviceReport(
                deviceIp = webSocketService?.getIpAddress(),
                apkPath = apkPath.absolutePath,
                apkSize = apk.size,
                apkName = apkName,
                status = false,
                bundle = bundle,
            )
        }
        webSocketService?.sendDownloadStatus(deviceReport)
        for (file in files) {
            val finalFile = File(outputDir, file.path)
            if (finalFile.exists() && finalFile.length() == file.size)
            {
                val hash = sha256(finalFile)
                if (hash == file.sha256)
                {
                    Log.e("DEPLOY", "Archivo ya completo, saltando: ${file.path}")
                    continue // No descargar
                }
                else
                {
                    finalFile.delete()
                    Log.e("DEPLOY","${file.path} está corrupto o ha cambiado, volviendo a descargar")
                }
            }
            Log.e("DEPLOY", "Descargando ${file.path}")
            deviceReport.currentFile = "(${files.indexOf(file) + 1}/${files.size}) - ${finalFile.name}"
            downloadFile(baseUrl, file, outputDir, deviceReport)
        }
        Log.e("DEPLOY", "Descarga completa")
        deviceReport.status = true
    }
    private suspend fun downloadFile(
        baseUrl: String,
        file: ManifestFile,
        outputDir: File,
        deviceReport: DeviceReport
    ) {
        withContext(Dispatchers.IO) {
            var maxRetries = 3
            val outputFile = File(outputDir, file.path)
            outputFile.parentFile?.mkdirs()

            while (maxRetries > 0)
            {
                maxRetries--
                // Archivo temporal para resumir descargas interrumpidas
                val tempFile = File(outputFile.parentFile, "${outputFile.name}.part")
                var downloaded: Long = if (tempFile.exists()) tempFile.length() else 0L

                val total = file.size

                val request = Request.Builder()
                    .url("$baseUrl/${file.path}")
                    .apply {
                        if (downloaded > 0) {
                            addHeader("Range", "bytes=$downloaded-")
                        }
                    }
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("DEPLOY", "Error descargando ${file.path}: ${response.code}")
                        continue
                    }
                    if (downloaded > 0 && response.code != 206)
                    {
                        tempFile.delete()
                        downloaded = 0
                    }
                    response.body.byteStream().use { input ->
                        FileOutputStream(tempFile, true).use { output ->
                            val buffer = ByteArray(1024 * 1024) // 1MB buffer
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead

                                val percent = (downloaded * 100 / total).toInt()
                                Log.e("DEPLOY", "Progreso ${file.path}: $percent%")
                                deviceReport.percent = percent
                            }
                            output.fd.sync()
                        }
                    }
                }
                if (tempFile.length() == file.size && sha256(tempFile) == file.sha256)
                {
                    tempFile.renameTo(outputFile)
                    Log.e("DEPLOY", "Descarga de ${file.path} completada")
                    return@withContext
                }
                tempFile.delete()
                Log.e("DEPLOY", "Intento fallido para ${file.path}")
            }
            Log.e("DEPLOY", "Máximos intentos alcanzados para ${file.path}")
        }
    }
}