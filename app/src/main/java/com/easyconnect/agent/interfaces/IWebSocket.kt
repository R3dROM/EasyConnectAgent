package com.easyconnect.agent.interfaces

interface IWebSocket {
    fun startWebSocketClient()
    fun createClient()
    fun connect() : Boolean
    fun disconnect()
    fun flushQueue()
    fun sendMessage(message: String)
    fun isConnected() : Boolean
    fun retryConnection()
}