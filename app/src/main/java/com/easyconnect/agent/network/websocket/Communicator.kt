package com.easyconnect.agent.network

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.easyconnect.agent.data.queue.DownloadReportPublisher
import com.easyconnect.agent.model.MessageStatus
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.interfaces.IWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class Communicator(
    private var url : String,
    private val registerJobId: Long,
    private val context: Context
) : IWebSocket
{
    private var client: OkHttpClient ?= null
    private var socket : WebSocket? = null
    private var isConnected = false
    private var serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO)

    private var socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i("WEB_SOCKET", "CONNECTED SUCCESSFUL")
            isConnected = true
            startConnectionMessage()
            sendReportInfo()
            startHeartbeat()
            startBattery()
            flushQueue()
        }
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.i("WEB_SOCKET","📩 Mensaje recibido: $text")
        }
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.i("WEB_SOCKET","📦 Mensaje binario recibido")
        }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            isConnected = false
            disconnect()
            Log.i("WEB_SOCKET","🔌 Cerrando conexión")
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            isConnected = false
            Log.i("WEB_SOCKET","❌ Error: ${t.message}")
            disconnect()
        }
    }
    private var heartBeatJob: Job? = null
    private var batteryJob: Job? = null
    private var downloadStatusJob: Job? = null
    private var messageQueue = ConcurrentLinkedQueue<String>()
    private var connectionRetries = 3
    override fun startWebSocketClient() {
        createClient()
        connect()
    }

    override fun createClient()
    {
        client = OkHttpClient.Builder()
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
    }
    override fun connect(): Boolean {
        try {
            Log.i("WEB_SOCKET_CONNECTION", "Trying connection to : " + this.url)
            val request = Request.Builder()
                .url(url)
                .addHeader("key", "PICO")
                .build()
            socket = client?.newWebSocket(request, socketListener)
            socket?.let {
                Log.i("WEB_SCOKET","Socket Connected")
                return true
            }
        } catch (e: SocketTimeoutException) {
            // receive() superó el SO_TIMEOUT
            Log.e("WEB_SOCKET","SocketTimeoutException: $e")
        } catch (e: SocketException) {
            // socket cerrado, dirección/puerto inválido,
            // problema de red a nivel de socket, etc.
            Log.e("WEB_SOCKET","SocketException: $e")
        } catch (e: SecurityException)
        {
            Log.e("WEB_SOCKET","SecurityException: $e")
        } catch (e: IOException) {
            // error de I/O
            Log.e("WEB_SOCKET","IOException: $e")
        }
        return false
    }
    override fun disconnect() {
        socket?.close(1000, "Cierre normal")
        socket = null
        isConnected = false
        connectionRetries = 3
        downloadStatusJob?.cancel()
        heartBeatJob?.cancel()
        batteryJob?.cancel()
        Log.i("WEB_SOCKET", "Socket Disconnected")
    }

    override fun isConnected(): Boolean =
        isConnected

    override fun retryConnection() {

    }

    override fun flushQueue()
    {
        while (messageQueue.isNotEmpty())
        {
            val msg = messageQueue.poll()
            if (msg != null) {
                sendMessage(msg)
            }
        }
    }
    override fun sendMessage(message: String) {
        if (!isConnected)
            messageQueue.add(message)
        else
            socket?.send(message)
    }

    private fun startConnectionMessage()
    {
        val json = """
                    {
                      "type":"${MessageType.Register}",
                      "payload":{
                        "ip":"${PersistentData.agentConfigurationReader.ip}",
                        "serialNumber": "${PersistentData.agentConfigurationReader.serialNumber}",
                        "deviceNumber": "${PersistentData.agentConfigurationReader.deviceNumber}",
                        "status": "${MessageStatus.Complete}",
                        "jobId": $registerJobId
                      }
                    }
                """.trimIndent()
        sendMessage(json)
    }
    @OptIn(ExperimentalTime::class)
    fun startHeartbeat() {
        heartBeatJob?.cancel()

        heartBeatJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive && isConnected) {
                val payload = JSONObject().apply {
                    put("ip", PersistentData.agentConfigurationReader.ip)
                    put("dateTime", Clock.System.now())
                }
                val json = JSONObject().apply {
                    put("type", MessageType.Heartbeat)
                    put("payload", payload)
                }
                sendMessage(json.toString())
                delay(15000.milliseconds)
            }
        }
    }
    private fun startBattery() {
        batteryJob?.cancel()
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

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
                        "ip":"${PersistentData.agentConfigurationReader.ip}",
                        "batteryLvl":$level
                      }
                    }
                """.trimIndent()

                sendMessage(json)
                delay(5000.milliseconds)
            }
        }
    }
    fun sendReportInfo()
    {
        downloadStatusJob?.cancel()

        downloadStatusJob = serviceScope.launch(Dispatchers.IO)
        {
            while (isActive)
            {
                val report = DownloadReportPublisher.receive()
                val json = """
                    {
                      "type":"${MessageType.Download}",
                      "payload":{
                        "ip": "${PersistentData.agentConfigurationReader.ip}",
                        "serialNumber": "${PersistentData.agentConfigurationReader.serialNumber}",
                        "status": "${report.status}",
                        "bundle": "${report.bundle}",
                        "apkName": "${report.apkName}",
                        "apkSize": ${report.apkSize},
                        "timestamp": ${report.timestamp},
                        "percent": ${report.percent},
                        "currentFile": "${report.currentFile}",
                        "jobId": ${report.jobId}
                      }
                    }
                """.trimIndent()
                sendMessage(json)
            }
        }
    }
}