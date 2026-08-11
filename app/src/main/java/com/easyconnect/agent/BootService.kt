package com.easyconnect.agent

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BootNotifier (
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Log.e("BOOT_RECEIVER", "Starting doWork!")
        Log.e("BOOT_RECEIVER", "======= WORKER STARTED======")

        return try {
            PersistentData.getAgentConfigs()
            val payload = JSONObject().apply {
                put("serialNumber", DeviceInfo.serialNumber)
                put("deviceId", DeviceInfo.deviceNumber)
                put("ip", DeviceInfo.ip)
                put("status", MessageStatus.Boot)
            }
            val json = JSONObject().apply {
                put("type", MessageType.Register)
                put("payload", payload)
            }
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("http://${DeviceInfo.serverConnectionIp}:${DeviceInfo.serverPort}/connect")
                .header("Content-Type", "application/json")
                .post(
                    body
                )
                .build()

            Log.e("BOOT_WORKER", "POST -> ${request.url}")
            client.newCall(request).execute().use { response ->

                Log.e(
                    "BOOT_WORKER",
                    "HTTP ${response.code}"
                )
                Log.e(
                    "BOOT_WORKER",
                    "Response: ${response.body?.string()}"
                )
                if (!response.isSuccessful)
                    return Result.retry()
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(
                "BOOT_WORKER",
                "POST exception",
                e
            )
            Result.retry()
        }
    }
}