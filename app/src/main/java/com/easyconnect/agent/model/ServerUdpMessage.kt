package com.easyconnect.agent.model

import kotlinx.serialization.Serializable

@Serializable
class ServerUdpMessage {
    val status: String ?= null
    val portWebSocket: String ?= null
    val portDownloads: String ?= null
    val ipServer: String ?= null
}