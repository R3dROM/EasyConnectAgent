package com.easyconnect.agent.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentConfigurationJsonData(
    var ip : String = "NO IP",
    var port: String = "5555",
    var serverConnectionIp : String = "NO SERVER",
    var serverPort : String = "7777",
    var serialNumber: String = "XXX",
    var deviceNumber: String = "NO NUMBER",
)