package com.easyconnect.agent.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.easyconnect.agent.configuration.agent.AgentConfiguration
import com.easyconnect.agent.configuration.download.DownloadConfiguration
import com.easyconnect.agent.configuration.network.NetworkConfiguration
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.deployment.DeploymentClass
import com.easyconnect.agent.deployment.download.DownloadReport
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.model.AgentStages
import com.easyconnect.agent.model.AgentStagesManager
import com.easyconnect.agent.network.AsyncUdpClient
import com.easyconnect.agent.pico.PicoConfigManager
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
    private var udpListener = AsyncUdpClient()
    private fun startWebSocketService(url: String, port: String)
    {
        val targetIntent = Intent().apply {
            component = ComponentName(
                AgentConfiguration.PACKAGE_NAME,
                AgentConfiguration.WEBSOCKET_CLASS_NAME
            )
            putExtra(NetworkConfiguration.WEBSOCKET_URL, url)
            putExtra(NetworkConfiguration.WEBSOCKET_PORT, port)
        }
        picoManager?.picoActivityService?.startService(targetIntent)
    }
    suspend fun startExperience(intent: Intent)
    {
        val appName = intent.getStringExtra("bundle") ?: DownloadReport.bundle

        val result = picoManager?.picoActivityService?.startActivity(
            appName,
            )
        Log.i("ACTIVITY", "Result: $result")
    }
    suspend fun startDeploymentService(intent: Intent)
    {
        val url = intent.getStringExtra(DownloadConfiguration.DOWNLOAD_URL) ?: ""
        val jobId = intent.getLongExtra(DownloadConfiguration.DOWNLOAD_JOB_ID, -1)
        val bundle = intent.getStringExtra(DownloadConfiguration.BUNDLE) ?: ""

        deployment = DeploymentClass(
            url,
            bundle,
            jobId,
            appContext,
        )
        Log.i("DEPLOYMENT_SERVICE", "STARTING DEPLOYMENT")
        deployment?.start()
    }
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

            AgentStagesManager.state = AgentStages.Connected
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
                delay(10000.milliseconds)
                val configs = compareConfig()
                waitForServer()
                PersistentData.agentConfigurationWriter.updateClientData(
                    ipClient = picoManager?.picoInformationService?.getIpAddress()
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
            val oldConfig = picoManager?.picoStore?.getPicoConfig()
            Log.e("PICO_MANAGER", "OLD MDM Configurations: ${picoManager?.picoStore?.getPicoConfig()}")
            val newConfig = PersistentData.picoConfigurationReader
            Log.e("PICO_MANAGER", "NEW MDM Configurations: ${newConfig.wifiSSID}")

            Log.i("PICO_MANAGER", "${picoManager?.picoInformationService?.getWifiStatus()}")
            Log.i("PICO_MANAGER", "${picoManager?.picoInformationService?.getPUIVersion()}")
            Log.e("DEVICE OWNER", "New Device Owner: ${picoManager?.picoControlAPP?.setDeviceOwner()}")
            if (newConfig.configChange)
            {
                Log.e("PICO_CONFIGURATION", "Default Wifi Connection: ${picoManager?.picoWifiService?.setDefaultWifiConnection(newConfig.wifiSSID, newConfig.wifiPassword)}")
                Log.e("PICO_CONFIGURATION", "Keep Wifi On: ${picoManager?.picoWifiService?.setKeepWifi(newConfig.keepWifiOn)}")

                Log.e("PICO_CONFIGURATION", "Wireless Debug: ${picoManager?.picoDebugService?.setWirelessDebug(newConfig.wirelessDebug)}")
                Log.e("PICO_CONFIGURATION", "Usb Debug: ${picoManager?.picoDebugService?.setUsbDebug(newConfig.usbDebug)}")

                Log.e("PICO_CONFIGURATION", "Fence Color: ${picoManager?.picoLbeService?.setFenceColor(newConfig.colorFence)}")
                Log.e("PICO_CONFIGURATION", "Distance Sensitivity: ${picoManager?.picoLbeService?.setDistanceSensitivity(newConfig.distanceSensitivityPlayBoundary)}")
                if (newConfig.needBoot)
                {
                    Log.e("PICO_CONFIGURATION", "Sleep Delay: ${picoManager?.picoUtilitiesService?.setSleepDelay()}")
                    Log.e("PICO_CONFIGURATION", "Screen Off Delay ${picoManager?.picoUtilitiesService?.setScreenOffDelay()}")
                    Log.e("PICO_CONFIGURATION", "Auto Sleep: ${picoManager?.picoUtilitiesService?.setAutoSleep()}")
                    Log.e("PICO_CONFIGURATION", "Skip OOBE: ${picoManager?.picoUtilitiesService?.skipOOBE()}")

                    Log.e("PICO_CONFIGURATION", "Home Gesture Disable: ${picoManager?.picoLbeService?.disableHomeGesture()}")
                    Log.e("PICO_CONFIGURATION","Long Home Button Press Disable: ${picoManager?.picoLbeService?.disableLongHomePress()}")
                    Log.e("PICO_CONFIGURATION", "Marker Detection: ${picoManager?.picoLbeService?.setMarkerDetection()}")
                    Log.e("PICO_CONFIGURATION", "Marker First: ${picoManager?.picoLbeService?.setMarkerFirst()}")
                    Log.e("PICO_CONFIGURATION", "Disable Boundary Pop Up Confirmation: ${picoManager?.picoLbeService?.disableBoundaryConfirmationPopUp()}")

                    Log.e("PICO_CONFIGURATION", "Hand And Controller: ${picoManager?.picoLbeService?.setHandAndController(newConfig.handAndControllers)}")
                    Log.e("PICO_CONFIGURATION", "Hand Tracking: ${picoManager?.picoLbeService?.setHandTracking(newConfig.handAndControllers)}")

                    val result = picoManager?.picoStore?.savePicoConfig(newConfig)
                    Log.e("PICO_MANAGER", "Device is going to reboot, config: ${picoManager?.picoStore?.getPicoConfig()}")
                    delay(30000.milliseconds)
                    picoManager?.picoUtilitiesService?.rebootPico()
                }
            }
            Log.e("PICO_MANAGER", "Device MDM Configurations: ${picoManager?.picoStore?.getPicoConfig()}")
            Log.e("PICO_MANAGER", "Import of map: ${picoManager?.picoLbeService?.importMap()}")
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