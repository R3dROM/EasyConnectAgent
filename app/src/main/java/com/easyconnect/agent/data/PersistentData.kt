package com.easyconnect.agent.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.easyconnect.agent.dependency.AgentDependencies
import kotlinx.serialization.json.Json
import java.io.File

const val NETWORKING_FILE = "AgentConfiguration.json"
const val PICO_FILE = "PicoConfiguration.json"
@kotlinx.serialization.Serializable
data class AgentConfigurationJsonData(
    var ip : String = "NO IP",
    var port: String = "5555",
    var serverConnectionIp : String = "NO SERVER",
    var serverPort : String = "7777",
    var serialNumber: String = "XXX",
    var deviceNumber: String = "NO NUMBER",
)
@kotlinx.serialization.Serializable
data class PicoConfigurationJsonData (
    val wifiSSID: String = "",
    val wifiPassword: String = "",
    val keepWifiOn: Boolean = false,
    val wirelessDebug: Boolean = false,
    val usbDebug: Boolean = false,

    val homeGesture: Boolean = false,
    val calibrateGesture: Boolean = false,
    val handAndControllers: Boolean = false,

    val modeLBE: Boolean = false,
    val longHomePressButton: Boolean = false,
    val temporaryPlayBoundary: Boolean = false,
    val boundaryConfirmationPopup: Boolean = false,
    val useMarkersFirstPositionRecovery: Boolean = false,
    val distanceSensitivityPlayBoundary: Int = 0,
    val colorFence: Triple<Int, Int, Int> = Triple(0,0,0)
)
object PersistentData  {
    private lateinit var agentConfigurationFile: File
    private  lateinit var picoConfigurationFile: File
    var picoConfigurationWriter = AgentDependencies.getPicoConfigurationWriter()
    var picoConfigurationReader = AgentDependencies.getPicoConfigurationReader()
    var agentConfigurationWriter = AgentDependencies.getAgentConfigurationWriter()
    var agentConfigurationReader = AgentDependencies.getAgentConfigurationReader()

    fun init(context: Context, oldConfig: PicoConfigurationJsonData?)
    {
        val externalDir = context.applicationContext.getExternalFilesDir(null)
        agentConfigurationFile = File(externalDir, NETWORKING_FILE)
        picoConfigurationFile = File(externalDir, PICO_FILE)
        
        setPermissionToFile(agentConfigurationFile)
        setPermissionToFile(picoConfigurationFile)

        getAgentConfigs()
        getPicoConfigurations(oldConfig)
    }
    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    fun setPermissionToFile(file: File)
    {
        if(!file.exists())
        {
            Log.e("READ_CONFIGURATION","NetworkingConfiugration doesnt exists, jump to fallbacks")
            return
        }
        file.setReadable(true, false)
        file.setWritable(true, false)
        file.setExecutable(true, false)
    }
    fun getAgentConfigs()
    {
        when {
            agentConfigurationFile.exists() -> {
                val config =
                    Json.decodeFromString<AgentConfigurationJsonData>(agentConfigurationFile.readText())
                agentConfigurationWriter.updateClientData(
                    "-1",
                    config.serverConnectionIp,
                    config.serverPort,
                    config.serverPort,
                    config.serialNumber,
                    config.deviceNumber
                )
            }
        }
    }
    fun getPicoConfigurations(oldConfig: PicoConfigurationJsonData?)
    {
        when
        {
            picoConfigurationFile.exists() -> {
                val newConfig = Json.decodeFromString<PicoConfigurationJsonData>(picoConfigurationFile.readText())
                var toChange : PicoConfigurationJsonData
                var configChange: Boolean
                when (oldConfig != newConfig)
                {
                    true -> {
                        toChange = newConfig
                        configChange = true
                    }
                    false -> {
                        toChange = oldConfig
                        configChange = false
                    }
                }
                picoConfigurationWriter.updateClientData(
                    wifiSSID = toChange.wifiSSID,
                    wifiPassword = toChange.wifiPassword,
                    wirelessDebug = toChange.wirelessDebug,
                    modeLBE = toChange.modeLBE,
                    usbDebug = toChange.usbDebug,
                    boundaryConfirmationPopup = toChange.boundaryConfirmationPopup,
                    calibrateGesture = toChange.calibrateGesture,
                    homeGesture = toChange.homeGesture,
                    handAndControllers = toChange.handAndControllers,
                    useMarkersFirstPositionRecovery = toChange.useMarkersFirstPositionRecovery,
                    keepWifiOn = toChange.keepWifiOn,
                    longHomePressButton = toChange.longHomePressButton,
                    temporaryPlayBoundary = toChange.temporaryPlayBoundary,
                    distanceSensitivityPlayBoundary = toChange.distanceSensitivityPlayBoundary,
                    colorFence = toChange.colorFence,
                    configChange = configChange
                )
            }
        }
    }
}