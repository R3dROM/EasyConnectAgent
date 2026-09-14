package com.easyconnect.agent.interfaces

interface IAgentConfigurationWriter {
    fun updateClientData(
        ipClient: String? = null,
        ipServer: String? = null,
        portDownloads: String? = null,
        portWebSocket: String? = null,
        serialNumber: String? = null,
        deviceNumber: String? = null)
}