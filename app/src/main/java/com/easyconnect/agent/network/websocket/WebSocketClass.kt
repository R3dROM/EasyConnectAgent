package com.easyconnect.agent.network.websocket

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.easyconnect.agent.configuration.agent.AgentConfiguration
import com.easyconnect.agent.data.queue.Communicator
import com.easyconnect.agent.model.DeviceStatus
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.interfaces.IInterpreter
import com.easyconnect.agent.interfaces.IWebSocket
import com.easyconnect.agent.network.connectionManager.ConnectionManager
import com.easyconnect.agent.network.report.Report
import com.easyconnect.agent.utilities.JsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class WebSocketClass(
    private var url : String,
    private val registerJobId: Long,
    private val interpreter: IInterpreter,
    private val context: Context
) : IWebSocket
{
    private var client: OkHttpClient ?= null
    private var socket : WebSocket? = null
    private var serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO)

    private var socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i("WEB_SOCKET", "CONNECTED SUCCESSFUL")
            sendReport()
            serviceScope.launch {
                startConnectionMessage()
                startHeartbeat()
                startBattery()
            }
            flushQueue()
        }
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.i("WEB_SOCKET","📩 Mensaje recibido: $text")
            interpreter.decodeToCommand(text)
        }
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.i("WEB_SOCKET","📦 Mensaje binario recibido")
        }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            ConnectionManager.setConnected(false)
            disconnect()
            Log.i("WEB_SOCKET","🔌 Cerrando conexión")
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            ConnectionManager.setConnected(false)
            Log.i("WEB_SOCKET","❌ Error: ${t.message} response: $response")
        }
    }
    private var heartBeatJob: Job? = null
    private var batteryJob: Job? = null
    private var reportInformationJob: Job? = null
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
                .addHeader("serial", PersistentData.agentConfigurationReader.serialNumber)
                .build()
            socket = client?.newWebSocket(request, socketListener)
            socket?.let {
                Log.i("WEB_SCOKET","Socket Connected")
                ConnectionManager.setConnected(true)
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
        ConnectionManager.setConnected(false)
        connectionRetries = 3
        reportInformationJob?.cancel()
        heartBeatJob?.cancel()
        batteryJob?.cancel()
        Log.i("WEB_SOCKET", "Socket Disconnected")
    }
    override fun isConnected(): Boolean =
        ConnectionManager.isConnected.value
    override fun retryConnection() {
        if (--connectionRetries <= 0)
            disconnect()
        else
            startWebSocketClient()
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
        if (!isConnected())
            messageQueue.add(message)
        else
            socket?.send(message)
    }
    private suspend fun startConnectionMessage()
    {
        val payload = JsonBuilder.putExtras(
            JsonBuilder.extra("ip", PersistentData.agentConfigurationReader.ip),
            JsonBuilder.extra("serialNumber", PersistentData.agentConfigurationReader.serialNumber),
            JsonBuilder.extra("deviceNumber", PersistentData.agentConfigurationReader.deviceNumber.toInt()),
            JsonBuilder.extra("puiVersion", PersistentData.picoConfigurationReader.puiVersion),
            JsonBuilder.extra("status", DeviceStatus.Online)
        )
        val report = Report(
            id = PersistentData.agentConfigurationReader.serialNumber,
            type = MessageType.Register,
            payload = payload
        )
        Communicator.publishReport(report)
    }
    private fun startHeartbeat() {
        heartBeatJob?.cancel()

        heartBeatJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive && isConnected()) {
                val time = Clock.System.now()

                val payload = JsonBuilder.putExtras(
                    JsonBuilder.extra("dateTime", time.nanosecondsOfSecond)
                )

                val report = Report(
                    id = PersistentData.agentConfigurationReader.serialNumber,
                    type = MessageType.Heartbeat,
                    payload = payload
                )
                Communicator.publishReport(report)
                delay(15000.milliseconds)
            }
        }
    }
    private  fun startBattery() {
        batteryJob?.cancel()
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        batteryJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive && isConnected())
            {
                val level = batteryManager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

                val payload = JsonBuilder.putExtras(
                    JsonBuilder.extra("batteryLvl", level)
                )

                val report = Report(
                    id = PersistentData.agentConfigurationReader.serialNumber,
                    type = MessageType.Battery,
                    payload = payload
                )
                Communicator.publishReport(report)
                delay(5000.milliseconds)
            }
        }
    }
    fun sendReport()
    {
        reportInformationJob?.cancel()

        reportInformationJob = serviceScope.launch(Dispatchers.IO)
        {
            while (isActive && isConnected())
            {
                val report = Communicator.receiveReport()

                val toJson = report.toJson()
                sendMessage(Json.encodeToString(toJson))
            }
        }
    }
}