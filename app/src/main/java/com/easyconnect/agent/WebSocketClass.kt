package com.easyconnect.agent

import android.annotation.SuppressLint
import android.content.Context
import android.os.BatteryManager
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.serialization.json.Json
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AgentConfig(
    val DeviceId: String = "",
    val SerialNumber: String = "",
    val Port: String = "",
    val FallBackIp: String = "",
    val SecondsToClick: String = ""
)
data object DeviceInfo
{
    var ip : String = "NO IP"
    var MAC: String = "NO MAC"
    var port: String = "5555"
    var serialNumber: String = "XXX"
    var deviceNumber: String = "NO NUMBER"
}
class WebSocketClass(
    private val client: OkHttpClient,
    private val url : String,
    private val registerJobId: Long,
    context: Context)
{
    private var heartBeatJob: Job? = null
    private var batteryJob: Job? = null
    private var downloadStatusJob: Job? = null
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var messageQueue = ConcurrentLinkedQueue<String>()
    private val ctx = context
    private var isConnected = false
    private var connectionRetries = 3;

    fun getSerialNumber(): String
    {
        return DeviceInfo.serialNumber
    }
    fun getDeviceIp() : String
    {
        return DeviceInfo.ip
    }
    fun getDeviceMAC() : String
    {
        return DeviceInfo.MAC
    }
    fun connect() {
        startDeviceInfo()
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(request, socketListener)
    }
    fun disconnect() {
        connectionRetries = 3
        isConnected = false
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
        serviceScope.cancel()
    }
    fun isConnected(): Boolean
    {
        return isConnected
    }
    fun flushQueue()
    {
        while (messageQueue.isNotEmpty())
        {
            val msg = messageQueue.poll()
            if (msg != null) {
                sendMessage(msg)
            }
        }
    }
    fun sendMessage(message: String) {
        if (!isConnected)
            messageQueue.add(message)
        else
            webSocket?.send(message)
    }
    private fun startConnectionMessage()
    {
        val json = """
                    {
                      "type":"${MessageType.Register}",
                      "payload":{
                        "ip":"${DeviceInfo.ip}",
                        "serialNumber": "${DeviceInfo.serialNumber}",
                        "deviceNumber": "${DeviceInfo.deviceNumber}",
                        "status": "${MessageStatus.Complete}",
                        "jobId": $registerJobId
                      }
                    }
                """.trimIndent()
        sendMessage(json)
    }
    fun startHeartbeat() {
        heartBeatJob?.cancel()

        heartBeatJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive && isConnected) {
                sendMessage("""{"type":"${MessageType.Heartbeat}"}""")
                delay(15000.milliseconds)
            }
        }
    }
    private fun startBattery() {
        batteryJob?.cancel()
        val batteryManager =
            ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        batteryJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive && isConnected)
            {
                val level = batteryManager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

                val json = """
                    {
                      "type":"${MessageType.Battery}",
                      "payload":{
                        "ip":"${DeviceInfo.ip}",
                        "batteryLvl":$level
                      }
                    }
                """.trimIndent()

                sendMessage(json)
                delay(5000.milliseconds)
            }
        }
    }
    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isConnected = true

            println("✅ Conectado al servidor")
            println("MAC: ${DeviceInfo.MAC}")
            println("IP: ${DeviceInfo.ip}")
            println("SERIAL NUMBER: ${DeviceInfo.serialNumber}")
            println("DEVICE NUMBER: ${DeviceInfo.deviceNumber}")
            startConnectionMessage()
            startHeartbeat()
            startBattery()
            flushQueue()
        }
        override fun onMessage(webSocket: WebSocket, text: String) {
            println("📩 Mensaje recibido: $text")
        }
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("📦 Mensaje binario recibido")
        }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            isConnected = false
            disconnect()
            println("🔌 Cerrando conexión")
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            isConnected = false
            println("❌ Error: ${t.message}")
            disconnect()
        }
    }
    fun downloadInfo( downloadReport: DownloadReport)
    {
        downloadStatusJob?.cancel()

        downloadStatusJob = serviceScope.launch(Dispatchers.IO)
        {
            while (isActive && isConnected)
            {
                val json = """
                    {
                      "type":"${MessageType.Download}",
                      "payload":{
                        "ip": "${DeviceInfo.ip}",
                        "serialNumber": "${DeviceInfo.serialNumber}",
                        "status": "${downloadReport.status}",
                        "bundle": "${downloadReport.bundle}",
                        "apkName": "${downloadReport.apkName}",
                        "apkSize": ${downloadReport.apkSize},
                        "timestamp": ${downloadReport.timestamp},
                        "percent": ${downloadReport.percent},
                        "currentFile": "${downloadReport.currentFile}",
                        "jobId": ${downloadReport.jobId}
                      }
                    }
                """.trimIndent()
                sendMessage(json)
                if (downloadReport.status == MessageStatus.Complete || downloadReport.status == MessageStatus.Cancel)
                {
                    downloadComplete()
                }
                delay(1000.milliseconds)
            }
        }
    }
    fun downloadComplete()
    {
        downloadStatusJob?.cancel()
        downloadStatusJob = null
    }
    private fun getIpAddress(): String
    {
        NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
            if (!networkInterface.isUp || networkInterface.isLoopback) return@forEach
            networkInterface.inetAddresses.toList().forEach { address ->
                if (address is Inet4Address && !address.isLoopbackAddress && networkInterface.name == "wlan0")
                    return address.hostAddress ?: "NO IP"
            }
        }
        return "NO WLAN0"
    }
    @SuppressLint("HardwareIds")
    private fun getMAC(): String
    {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
    }

    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    private fun getAgentConfigs(): AgentConfig
    {
        val file = File(ctx.getExternalFilesDir(null), "NetworkingConfiguration.json")
        file.setReadable(true, false)
        file.setWritable(true, false)
        file.setExecutable(true, false)
        if (!file.exists())
        {
            return AgentConfig()
        }
        val config = Json.decodeFromString<AgentConfig>(
            file.readText()
        )
        return config
    }
    fun startDeviceInfo()
    {
        val agentConfig = getAgentConfigs()

        DeviceInfo.ip = getIpAddress()
        DeviceInfo.MAC = getMAC()
        DeviceInfo.serialNumber = agentConfig.SerialNumber
        DeviceInfo.deviceNumber = agentConfig.DeviceId
    }
}