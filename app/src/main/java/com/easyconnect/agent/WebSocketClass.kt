package com.easyconnect.agent

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentLinkedQueue

class WebSocketClass(
    private val client: OkHttpClient,
    private val url : String,
    context: Context)
{
    private var handlerThread: HandlerThread ? = null
    private var webSocket: WebSocket? = null
    private lateinit var customLooper : Looper
    private lateinit var customHandler : Handler
    lateinit var receiver: BroadcastReceiver
    private var messageQueue = ConcurrentLinkedQueue<String>()
    private val ctx = context
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var deviceMAC: String = "NO MAC"
    private var deviceIp: String = "NO IP"

    fun getDeviceIp() : String
    {
        return deviceIp
    }
    fun getDeviceMAC() : String
    {
        return deviceMAC
    }
    fun connect() {
        startDeviceInfo()
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(request, socketListener)
        handlerThread = HandlerThread("Download Messages information")
        handlerThread!!.start()
        customLooper = handlerThread!!.looper
        customHandler = Handler(customLooper)

        receiver = PackageInstallReceiver()
        val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED).apply {
//            addDataScheme("package")
        }
        registerReceiver(ctx, receiver, filter, RECEIVER_EXPORTED)
    }
    fun disconnect() {
        isConnected = false
        handler.removeCallbacksAndMessages(null)
        handlerThread?.quitSafely()
        handlerThread = null
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
        ctx.unregisterReceiver(receiver)
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
                      "type":"register",
                      "payload":{
                        "deviceId":"$deviceIp",
                        "appVersion":"1.0.0"
                      }
                    }
                """.trimIndent()
        sendMessage(json)
    }
    fun startHeartbeat() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isConnected) return
                sendMessage("""{"type":"heartbeat"}""")
                handler.postDelayed(this, 15000)
            }
        }, 15000)
    }
    private fun startBattery() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isConnected) return

                val batteryManager =
                    ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

                val level = batteryManager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

                val json = """
                    {
                      "type":"battery",
                      "payload":{"level":$level}
                    }
                """.trimIndent()

                sendMessage(json)

                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }
    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isConnected = true

            println("✅ Conectado al servidor")
            println("MAC: $deviceMAC")
            println("IP: $deviceIp")
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
            disconnect()
            println("🔌 Cerrando conexión")
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            isConnected = false
            println("❌ Error: ${t.message}")
            handler.postDelayed({
                connect()
            }, 5000)
        }
    }
    fun downloadInfo( deviceReport: DeviceReport)
    {
        customHandler.postDelayed(object : Runnable {
            override fun run() {
                if (!isConnected) return
                val json = """
                    {
                      "type":"downloadInformation",
                      "payload":{
                        "deviceId": "$deviceIp",
                        "status": ${deviceReport.status},
                        "bundle": "${deviceReport.bundle}",
                        "apkPath": "${deviceReport.apkPath}",
                        "apkName": "${deviceReport.apkName}",
                        "apkSize": ${deviceReport.apkSize},
                        "timestamp": ${deviceReport.timestamp},
                        "percent": ${deviceReport.percent},
                        "currentFile": "${deviceReport.currentFile}"
                      }
                    }
                """.trimIndent()
                sendMessage(json)
                if (deviceReport.status)
                {
                    downloadComplete()
                    return
                }
                customHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }
    fun downloadComplete()
    {
        customHandler.looper.quitSafely()
        handlerThread!!.quitSafely()
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
    fun startDeviceInfo()
    {
        deviceIp = getIpAddress()
        deviceMAC = getMAC()
    }
}