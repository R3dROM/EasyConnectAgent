package com.easyconnect.agent.configuration.agent

import com.easyconnect.agent.interfaces.IAgentConfigurationReader
import com.easyconnect.agent.interfaces.IAgentConfigurationWriter

object AgentConfiguration : IAgentConfigurationWriter, IAgentConfigurationReader{
    const val START_DEPLOYMENT_SERVICE = "START_DEPLOYMENT_SERVICE"
    const val STOP_DEPLOYMENT_SERVICE = "STOP_DEPLOYMENT_SERVICE"
    const val START_EXPERIENCE = "START_EXPERIENCE"
    const val START_PICO_CONFIGURATION = "START_PICO_CONFIGURATION"
    const val TARGET_SERVICE = "TARGET_SERVICE"
    const val PACKAGE_NAME = "com.easyconnect.agent"
    const val WEBSOCKET_CLASS_NAME = "com.easyconnect.agent.service.WebSocketService"

    override var ip : String = "NO IP"
        set(value) {
            if (field != value)
                field = value
        }
    override var portDownloads: String = "5555"
        set(value) {
            if (field != value)
                field = value
        }
    override var serverConnectionIp : String = "NO SERVER"
        set(value) {
            if (field != value)
                field = value
        }
    override var portWebSocket : String = "7777"
        set(value) {
            if (field != value)
                field = value
        }
    override var serialNumber: String = "XXX"
        set(value) {
            if (field != value)
                field = value
        }
    override var deviceNumber: String = "NO NUMBER"
        set(value) {
            if (field != value)
                field = value
        }

    override fun updateClientData(
        ipClient: String?,
        ipServer: String?,
        portDownloads: String?,
        portWebSocket: String?,
        serialNumber: String?,
        deviceNumber: String?
    ) {
        ipClient?.let { ip = it }
        ipServer?.let { serverConnectionIp = it }
        portDownloads?.let { AgentConfiguration.portDownloads = it }
        portWebSocket?.let { AgentConfiguration.portWebSocket = it }
        serialNumber?.let { AgentConfiguration.serialNumber = it }
        deviceNumber?.let { AgentConfiguration.deviceNumber = it }
    }
}