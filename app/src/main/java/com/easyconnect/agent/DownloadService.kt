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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.jvm.java
import kotlin.math.max

//data class Manifest(
//    val version: String,
//    val files: List<ManifestFile>
//)

data class ManifestFile(
    val path: String,
    val size: Long,
    val sha256: String
)
class DownloadService : Service()
{
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)      // conexión inicial
        .readTimeout(30, TimeUnit.MINUTES)        // lectura de bytes grandes
        .writeTimeout(30, TimeUnit.MINUTES)       // si se subiera algo
        .retryOnConnectionFailure(true)           // reintentos automáticos
        .build()
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

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
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
        var apkPath = ""
        println(baseUrl)
        val manifestRaw = downloadManifestRaw(manifestUrl)
            ?: return

        val files = parseManifest(manifestRaw)

        for (file in files) {
            val finalFile = File(outputDir, file.path)
            if (finalFile.exists() && finalFile.length() == file.size)
            {
                val hash = sha256(finalFile)
                if (hash == file.sha256)
                {
                    Log.d("DEPLOY", "Archivo ya completo, saltando: ${file.path}")
                    continue // No descargar
                }
                else
                {
                    finalFile.delete()
                    Log.d("DEPLOY","${file.path} está corrupto o ha cambiado, volviendo a descargar")
                }
            }
            Log.d("DEPLOY", "Descargando ${file.path}")

            downloadFile(baseUrl, file, outputDir,file.sha256)
            if (file.path.endsWith(".apk")) {
                val finalFile = File(outputDir, file.path)
                apkPath = finalFile.absolutePath
                Log.d("DEPLOY", "apkPath $apkPath")
                println("apkPath $apkPath")
            }
        }
        Log.d("DEPLOY", "Descarga completa")
        val installIntent = Intent(applicationContext, InstallService::class.java)
        installIntent.putExtra("apkPath", apkPath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(installIntent)
        } else {
            applicationContext.startService(installIntent)
        }
        sendStatus(getIpAddress(), bundle, "SUCCESS", baseUrl)
    }
    private fun getIpAddress(): String?
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
        hash: String
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
                                Log.d("DEPLOY", "Progreso ${file.path}: $percent%")
                            }
                            output.fd.sync()
                        }
                    }
                }
                if (tempFile.length() == file.size && sha256(tempFile) == file.sha256)
                {
                    tempFile.renameTo(outputFile)
                    Log.d("DEPLOY", "Descarga de ${file.path} completada")
                    return@withContext
                }
                tempFile.delete()
                Log.d("DEPLOY", "Intento fallido para ${file.path}")
            }
            Log.d("DEPLOY", "Máximos intentos alcanzados para ${file.path}")
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
    private suspend fun sendStatus(deviceId: String?, bundleId: String, status: String, ipServer: String) {
        val json = """
        {
            "deviceId": "$deviceId",
            "bundle": "$bundleId",
            "downloadStatus": "$status",
            "installStatus": "$status",
            "timestamp": ${System.currentTimeMillis()}
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$ipServer/report/")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            println("Servidor respondió: ${response.code}")
        }
    }
    override fun onBind(intent: Intent?) = null
}