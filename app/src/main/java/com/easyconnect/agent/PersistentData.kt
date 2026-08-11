package com.easyconnect.agent

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

data object DeviceInfo
{
    var ip : String = "NO IP"
    var port: String = "5555"
    var serverConnectionIp = "NO SERVER"
    var serverPort = "7777"
    var serialNumber: String = "XXX"
    var deviceNumber: String = "NO NUMBER"
}

object PersistentData {
    private lateinit var networkingConfiguration: File
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class AgentConfig(
        val DeviceId: String = "",
        val SerialNumber: String = "",
        val ServerPort: String = "",
        val ServerConnectionIp: String = "",
        val SecondsToClick: String = ""
    )

    private fun getIpAddress(): String
    {
        NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
            if (!networkInterface.isUp || networkInterface.isLoopback) return@forEach
            networkInterface.inetAddresses.toList().forEach { address ->
                if (address is Inet4Address && !address.isLoopbackAddress && networkInterface.name == "wlan0")
                    return address.hostAddress ?: "NO IP"
            }
        }
        return "NO WLAN0"
    }

    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    fun readNetworkingConfiguration(ctx: Context)
    {
        networkingConfiguration = File(ctx.getExternalFilesDir(null), "NetworkingConfiguration.json")
        networkingConfiguration.setReadable(true, false)
        networkingConfiguration.setWritable(true, false)
        networkingConfiguration.setExecutable(true, false)
    }
    fun getAgentConfigs()
    {
        when
        {
            networkingConfiguration.exists() -> {
                val config = Json.decodeFromString<AgentConfig>(networkingConfiguration.readText())

                DeviceInfo.serialNumber = config.SerialNumber
                DeviceInfo.deviceNumber = config.DeviceId
                DeviceInfo.serverConnectionIp = config.ServerConnectionIp
                DeviceInfo.serverPort = config.ServerPort
            }

            else -> {
                DeviceInfo.serialNumber = ""
                DeviceInfo.deviceNumber = ""
            }
        }
        DeviceInfo.ip = getIpAddress()
    }
}