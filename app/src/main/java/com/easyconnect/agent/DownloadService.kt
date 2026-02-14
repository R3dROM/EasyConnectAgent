package com.easyconnect.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.InetAddresses
import android.net.IpPrefix
import android.net.StaticIpConfiguration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

data class Manifest(
    val version: String,
    val files: List<ManifestFile>
)

data class ManifestFile(
    val path: String,
    val size: Long,
    val sha256: String
)
class DownloadService : Service()
{
    private val client = OkHttpClient()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotification()

        startForeground(1, createNotification())
        val baseUrl = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val bundle = intent.getStringExtra("bundle") ?: return START_NOT_STICKY

        val outputDir = File(getExternalFilesDir(null), "$bundle/files/")

        CoroutineScope(Dispatchers.IO).launch {
            downloadExperience(baseUrl, outputDir, bundle)
            stopSelf()
        }

        return START_NOT_STICKY
    }


    private suspend fun downloadManifestRaw(manifestUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val request = Request.Builder()
                    .url(manifestUrl)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                response.body?.string()

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseManifest(jsonString: String): List<ManifestFile> {

        val filesList = mutableListOf<ManifestFile>()

        val jsonObject = JSONObject(jsonString)
        val filesArray = jsonObject.getJSONArray("files")

        for (i in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(i)

            val path = item.getString("path")
            val size = item.getLong("size")
            val sha256 = item.getString("sha256")

            filesList.add(
                ManifestFile(path, size, sha256)
            )
        }

        return filesList
    }

    private suspend fun downloadExperience(baseUrl: String, outputDir: File, bundle: String) {

        val manifestUrl = "$baseUrl/manifest.json"

        println(baseUrl)
        val manifestRaw = downloadManifestRaw(manifestUrl)
            ?: return

        val files = parseManifest(manifestRaw)

        for (file in files) {

            val finalFile = File(outputDir, file.path)
            if (finalFile.exists() && finalFile.length() == file.size) {
                Log.d("DEPLOY", "Archivo ya completo, saltando: ${file.path}")
                continue // No descargar
            }
            Log.d("DEPLOY", "Descargando ${file.path}")

            downloadFile(baseUrl, file, outputDir, bundle)
        }

        Log.d("DEPLOY", "Descarga completa")
        sendStatus(getIpAddress(), bundle, "SUCCESS", baseUrl)
    }

    private fun getIpAddress(): String
    {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return ""
    }
    private suspend fun downloadFile(
        baseUrl: String,
        file: ManifestFile,
        outputDir: File,
        bundle: String
    ) {
        withContext(Dispatchers.IO) {

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)      // conexión inicial
                .readTimeout(30, TimeUnit.MINUTES)        // lectura de bytes grandes
                .writeTimeout(30, TimeUnit.MINUTES)       // si se subiera algo
                .retryOnConnectionFailure(true)           // reintentos automáticos
                .build()

            val outputFile = File(outputDir, file.path)
            outputFile.parentFile?.mkdirs()

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
                    return@withContext
                }

                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempFile, true).use { output ->
                        val buffer = ByteArray(1024 * 1024) // 1MB buffer
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val percent = (downloaded * 100 / total).toInt()
                            Log.d("DEPLOY", "Progreso ${file.path}: $percent%")
                        }
                    }
                }
            }

            // Cuando termina, renombrar el archivo final
            tempFile.renameTo(outputFile)
            Log.d("DEPLOY", "Descarga completa: ${file.path}")
        }
    }



    private fun createNotification(): Notification {
        val channelId = "deploy_channel"

        val channel = NotificationChannel(
            channelId,
            "Deploy",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Deploy", NotificationManager.IMPORTANCE_LOW)
            nm?.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("EasyDeploy")
            .setContentText("Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
    }

    private suspend fun sendStatus(deviceId: String, bundleId: String, status: String, ipServer: String) {
        println("Entro al status")
        val json = """
        {
            "deviceId": "$deviceId",
            "bundle": "$bundleId",
            "status": "$status",
            "timestamp": ${System.currentTimeMillis()}
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$ipServer/report/")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            println("Servidor respondió: ${response.code}")
        }
    }

    override fun onBind(intent: Intent?) = null

}