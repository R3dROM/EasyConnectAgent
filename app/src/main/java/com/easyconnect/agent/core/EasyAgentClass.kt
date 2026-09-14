package com.easyconnect.agent.core

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.easyconnect.agent.commandManager.CommandManager
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.data.queue.Communicator
import com.easyconnect.agent.deployment.DeploymentClass
import com.easyconnect.agent.utilities.DownloadReport
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.model.CommandType
import com.easyconnect.agent.model.JobState
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.network.AsyncUdpClient
import com.easyconnect.agent.commandManager.Command
import com.easyconnect.agent.network.interpreter.Interpreter
import com.easyconnect.agent.network.report.Report
import com.easyconnect.agent.pico.PicoConfigManager
import com.easyconnect.agent.utilities.JsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
class EasyAgentClass(
    private val appContext: Context
) {
    private var deployment : IDeployProcess ?= null
    private var picoManager : PicoConfigManager ?= null
    private var commandManager: CommandManager ?= null
    private var udpListener = AsyncUdpClient()

    /////////////////////////////
    // COMMAND MANAGER O INTERPRETER
    private fun startWebSocketService(url: String, port: String)
    {
        val extras = JsonBuilder.putExtras(
            JsonBuilder.extra(ActivityConfiguration.URL, url),
            JsonBuilder.extra(ActivityConfiguration.PORT, port)
        )
        val startWebSocketCommand = Command(
            id = 1,
            commandType = CommandType.Websocket,
            extras = extras
        )

        Log.i("COMMAND", "Publishing start web socket command: $startWebSocketCommand")
        Interpreter.publishCommand(startWebSocketCommand)
    }
    suspend fun startExperience(jobId: Long, commandType: CommandType, bundle: Bundle?)
    {
        val appName = bundle?.getString("bundle") ?: DownloadReport.bundle

        val result = picoManager?.picoActivityService?.startActivity(
            appName,
            )

        Log.i("ACTIVITY", "Result: $result")

        val payload = JsonBuilder.putExtras(
            JsonBuilder.extra("status", when
            {
                result == 0 -> JobState.Complete
                else -> JobState.Fail
            })
        )
        val report = Report(
            id = PersistentData.agentConfigurationReader.serialNumber,
            type = MessageType.Acknowledge,
            timestamp = 0,
            jobId = jobId,
            payload = payload
        )

        Communicator.publishReport(report)
    }
    suspend fun startDeploymentService(jobId: Long, extras: Bundle?)
    {
        val stop = extras?.getBoolean(ActivityConfiguration.CANCELLATION, false)
        if (stop == true)
        {
            deployment?.shutdown()
            return
        }

        deployment = DeploymentClass(
            extras,
            jobId,
            appContext
        )
        Log.i("DEPLOYMENT_SERVICE", "STARTING DEPLOYMENT")
        deployment?.start()
    }

    ////////////////////////////
    suspend fun bootAgent()
    {
        startBootConfiguration()
    }
    private suspend fun waitForServer()
    {
        withContext(Dispatchers.IO)
        {
            Log.i("UDP","Waiting for UDP broadcast to start the booting...")
            val messageFromServer = udpListener.startClient()
            Log.i("UDP","UDP broadcast received, starting process... BROADCAST: ${messageFromServer?.status}")
            PersistentData.agentConfigurationWriter.updateClientData(
                ipServer = messageFromServer?.ipServer,
                portDownloads = messageFromServer?.portDownloads,
                portWebSocket = messageFromServer?.portWebSocket
            )
        }
    }
    private suspend fun startBootConfiguration() {
        withContext(Dispatchers.IO)
        {
            if (picoManager == null)
                picoManager = PicoConfigManager(appContext)
            picoManager?.picoConnection {
                currentCoroutineContext().isActive
                PersistentData.init(appContext, picoManager?.picoStore?.getPicoConfig())
                val configs = compareConfig()
                waitForServer()
                PersistentData.agentConfigurationWriter.updateClientData(
                    ipClient = picoManager?.picoInformationService?.getIpAddress()
                )
                commandManager = CommandManager(
                    appContext,
                    picoManager?.picoActivityService!!
                )
                startWebSocketService(
                    PersistentData.agentConfigurationReader.serverConnectionIp,
                    PersistentData.agentConfigurationReader.portWebSocket)
            }
        }
    }
    suspend fun compareConfig()
    {
        withContext(Dispatchers.IO)
        {
//            val oldConfig = picoManager?.picoStore?.getPicoConfig()
//            Log.e("PICO_MANAGER", "OLD MDM Configurations: ${picoManager?.picoStore?.getPicoConfig()}")
//            val newConfig = PersistentData.picoConfigurationReader
//            Log.e("PICO_MANAGER", "NEW MDM Configurations: ${newConfig.wifiSSID}")
//
//            Log.i("PICO_MANAGER", "${picoManager?.picoInformationService?.getWifiStatus()}")
//            Log.i("PICO_MANAGER", "${picoManager?.picoInformationService?.getPUIVersion()}")
//            Log.e("DEVICE OWNER", "New Device Owner: ${picoManager?.picoControlAPP?.setDeviceOwner()}")
//            if (newConfig.configChange)
//            {
//                Log.e("PICO_CONFIGURATION", "Default Wi-Fi Connection: ${picoManager?.picoWifiService?.setDefaultWifiConnection(newConfig.wifiSSID, newConfig.wifiPassword)}")
//                Log.e("PICO_CONFIGURATION", "Keep Wi-Fi On: ${picoManager?.picoWifiService?.setKeepWifi(newConfig.keepWifiOn)}")
//
//                Log.e("PICO_CONFIGURATION", "Wireless Debug: ${picoManager?.picoDebugService?.setWirelessDebug(newConfig.wirelessDebug)}")
//                Log.e("PICO_CONFIGURATION", "Usb Debug: ${picoManager?.picoDebugService?.setUsbDebug(newConfig.usbDebug)}")
//
//                Log.e("PICO_CONFIGURATION", "Fence Color: ${picoManager?.picoLbeService?.setFenceColor(newConfig.colorFence)}")
//                Log.e("PICO_CONFIGURATION", "Distance Sensitivity: ${picoManager?.picoLbeService?.setDistanceSensitivity(newConfig.distanceSensitivityPlayBoundary)}")
//                if (newConfig.needBoot)
//                {
//                    Log.e("PICO_CONFIGURATION", "Sleep Delay: ${picoManager?.picoUtilitiesService?.setSleepDelay()}")
//                    Log.e("PICO_CONFIGURATION", "Screen Off Delay ${picoManager?.picoUtilitiesService?.setScreenOffDelay()}")
//                    Log.e("PICO_CONFIGURATION", "Auto Sleep: ${picoManager?.picoUtilitiesService?.setAutoSleep()}")
//                    Log.e("PICO_CONFIGURATION", "Skip OOBE: ${picoManager?.picoUtilitiesService?.skipOOBE()}")
//
//                    Log.e("PICO_CONFIGURATION", "Home Gesture Disable: ${picoManager?.picoLbeService?.disableHomeGesture()}")
//                    Log.e("PICO_CONFIGURATION","Long Home Button Press Disable: ${picoManager?.picoLbeService?.disableLongHomePress()}")
//                    Log.e("PICO_CONFIGURATION", "Marker Detection: ${picoManager?.picoLbeService?.setMarkerDetection()}")
//                    Log.e("PICO_CONFIGURATION", "Marker First: ${picoManager?.picoLbeService?.setMarkerFirst()}")
//                    Log.e("PICO_CONFIGURATION", "Disable Boundary Pop Up Confirmation: ${picoManager?.picoLbeService?.disableBoundaryConfirmationPopUp()}")
//
//                    Log.e("PICO_CONFIGURATION", "Hand And Controller: ${picoManager?.picoLbeService?.setHandAndController(newConfig.handAndControllers)}")
//                    Log.e("PICO_CONFIGURATION", "Hand Tracking: ${picoManager?.picoLbeService?.setHandTracking(newConfig.handAndControllers)}")
//
//                    val result = picoManager?.picoStore?.savePicoConfig(newConfig)
//                    Log.e("PICO_MANAGER", "Device is going to reboot, config: ${picoManager?.picoStore?.getPicoConfig()}")
//                    delay(30000.milliseconds)
//                    picoManager?.picoUtilitiesService?.rebootPico()
//                }
//            }
//            Log.e("PICO_MANAGER", "Device MDM Configurations: ${picoManager?.picoStore?.getPicoConfig()}")
//            Log.e("PICO_MANAGER", "Import of map: ${picoManager?.picoLbeService?.importMap()}")
            delay(10000.milliseconds)
        }
    }
    fun onDestroy()
    {
        picoManager?.picoDisconnection()
    }
    fun shutDown()
    {
        deployment?.shutdown()
    }
}