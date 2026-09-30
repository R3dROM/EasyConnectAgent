package com.easyconnect.agent.interfaces

import com.easyconnect.agent.model.ServerUdpMessage
import com.easyconnect.agent.network.connectionManager.ConnectionInfo

interface IUdpBroadcast {
    suspend fun startClient() : ConnectionInfo?
}