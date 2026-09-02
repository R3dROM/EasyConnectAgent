package com.easyconnect.agent.interfaces

import com.easyconnect.agent.model.ServerUdpMessage

interface IUdpBroadcast {
    suspend fun startClient() : ServerUdpMessage?
}