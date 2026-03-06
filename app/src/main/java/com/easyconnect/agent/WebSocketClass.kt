package com.easyconnect.agent

import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.ConcurrentLinkedQueue

class WebSocketClass(
    private val client: OkHttpClient,
    private val url : String,
    context: Context)
{
    private var messageQueue = ConcurrentLinkedQueue<String>()
    private val ctx = context
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(request, socketListener)
    }
    fun sendMessage(message: String) {
        if (!isConnected)
            messageQueue.add(message)
        else
            webSocket?.send(message)
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
    fun startHeartbeat() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isConnected) return
                webSocket?.send("""{"type":"heartbeat"}""")
                handler.postDelayed(this, 15000)
            }
        }, 15000)
    }
    fun disconnect() {
        isConnected = false
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "Cierre normal")
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

                webSocket?.send(json)

                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }
    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isConnected = true
            println("✅ Conectado al servidor")
            sendMessage("""
                    {
                      "type":"register",
                      "payload":{
                        "deviceId":"pico-01",
                        "appVersion":"1.0.0"
                      }
                    }
                """.trimIndent())
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
            webSocket.close(1000, null)
            println("🔌 Cerrando conexión")
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("❌ Error: ${t.message}")
        }
    }
}