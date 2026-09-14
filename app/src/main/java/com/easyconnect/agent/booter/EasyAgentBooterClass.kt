package com.easyconnect.agent.booter

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.easyconnect.agent.model.DeviceStatus
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.data.PersistentData
import kotlinx.coroutines.Job
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EasyAgentBooterClass (
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private var job : Job?= null

    override suspend fun doWork(): Result {
        return startEasyAgentService()
    }

    private fun startEasyAgentService(): Result {
        val bootReady : Boolean = bootConnection()
        if (!bootReady)
            return Result.retry()

        return Result.success()
    }
    suspend fun waitForUdpToConnect()
    {

    }
}
private fun bootConnection(): Boolean
{
    val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(0, TimeUnit.MINUTES)
        .writeTimeout(0, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .connectionPool(
            ConnectionPool(
                1,
                30,
                TimeUnit.SECONDS
            )
        )
        .build()
    Log.i("BOOT_RECEIVER", "Starting doWork!")
    Log.i("BOOT_RECEIVER", "======= WORKER STARTED======")
    try {
        val agent = PersistentData.agentConfigurationReader
        val payload = JSONObject().apply {
            put("serialNumber", agent.serialNumber)
            put("deviceId", agent.deviceNumber)
            put("ip", agent.ip)
            put("status", DeviceStatus.Boot)
        }
        val json = JSONObject().apply {
            put("type", MessageType.Register)
            put("payload", payload)
        }
        val body = json.toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://${agent.serverConnectionIp}:${agent.portWebSocket}/connect")
            .header("Content-Type", "application/json")
            .post(
                body
            )
            .build()

        Log.i("BOOT_WORKER", "POST -> ${request.url}")
        client.newCall(request).execute().use { response ->

            Log.i(
                "BOOT_WORKER",
                "HTTP ${response.code}"
            )
            Log.i(
                "BOOT_WORKER",
                "Response: ${response.body.string()}"
            )
            if (!response.isSuccessful)
                return false
        }

        return true

    } catch (e: Exception) {
        Log.e(
            "BOOT_WORKER",
            "POST exception",
            e
        )
        return false
    }
}