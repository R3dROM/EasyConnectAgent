package com.easyconnect.agent.interfaces

interface IAgentConfigurationReader {
    var ip : String
    var portDownloads: String
    var serverConnectionIp : String
    var portWebSocket : String
    var serialNumber: String
    var deviceNumber: String
}