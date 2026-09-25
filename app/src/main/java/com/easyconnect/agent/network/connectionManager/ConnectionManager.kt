package com.easyconnect.agent.network.connectionManager

import android.util.Log
import com.easyconnect.agent.commandManager.Command
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.model.CommandType
import com.easyconnect.agent.network.MDnsClient
import com.easyconnect.agent.network.interpreter.Interpreter
import com.easyconnect.agent.pico.PicoConfigManager
import com.easyconnect.agent.utilities.JsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ConnectionInfo(
    val serverIp: String,
    val serverPort: String,
    val webSocketPort: String
)
object ConnectionManager {
    private var _isConnected : MutableStateFlow<Boolean> = MutableStateFlow(false)
    var connectionInfo: ConnectionInfo ?= null
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun setConnected(value: Boolean)
    {
        _isConnected.value = value
    }

    suspend fun waitForServer(udpClient: MDnsClient): ConnectionInfo?
    {
        return withContext(Dispatchers.IO)
        {
            Log.i("UDP","Waiting for server to start ...")
            val messageFromServer = udpClient.startClient()
            Log.i("UDP","server found, starting process... Server Info: ${messageFromServer?.serverIp}")
            messageFromServer
        }
    }
    suspend fun startPicoConnection(picoManager: PicoConfigManager?): Boolean {
        return withContext(Dispatchers.IO)
        {
            val result = picoManager?.picoConnection()
            Log.i("PICO CONNECTION", "PICO CONNECTION STATUS: $result")
            result == true
        }
    }
    fun startWebSocketService(url: String, port: String)
    {
        val extras = JsonBuilder.putExtras(
            JsonBuilder.extra(ActivityConfiguration.URL, url),
            JsonBuilder.extra(ActivityConfiguration.PORT, port)
        )
        val startWebSocketCommand = Command(
            id = 1,
            commandType = CommandType.Connection,
            extras = extras
        )

        Log.i("COMMAND", "Publishing start web socket command: $startWebSocketCommand")
        Interpreter.publishCommand(startWebSocketCommand)
    }
}