package com.easyconnect.agent.core

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.easyconnect.agent.commandManager.CommandManager
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.data.queue.Communicator
import com.easyconnect.agent.deployment.DeploymentClass
import com.easyconnect.agent.utilities.DownloadReport
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.model.JobState
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.network.MDnsClient
import com.easyconnect.agent.model.ActivityType
import com.easyconnect.agent.model.CommandType
import com.easyconnect.agent.network.connectionManager.ConnectionManager
import com.easyconnect.agent.network.report.Report
import com.easyconnect.agent.pico.PicoConfigManager
import com.easyconnect.agent.utilities.JsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
class EasyAgentClass(
    private val appContext: Context
) {
    private val serviceScope =
        CoroutineScope(
        Dispatchers.IO + SupervisorJob())
    private var deployment : IDeployProcess ?= null
    private var picoManager : PicoConfigManager ?= null
    private var commandManager: CommandManager ?= null
    private var mDnsListener =
        MDnsClient(appContext)

    init {
        serviceScope.launch {
            if (picoManager == null)
                picoManager = PicoConfigManager(appContext)

            ConnectionManager.isConnected
                .collect { connected ->
                if (connected)
                    onConnected()
                else
                    onDisconnected()
            }
        }
    }
    /////////////////////////////
    // COMMAND MANAGER O INTERPRETER

    suspend fun startActivityManager(jobId: Long, extras: Bundle?, options: Bundle?)
    {
        Log.i("TYPE OF OPTIONS", "EXTRAS IS ${extras?.getString("bundle") ?: "NO"}")
        Log.i("TYPE BUILD", (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU).toString())
        var result: Int? = 0
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            options?.getSerializable(
                "type",
                ActivityType::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            options?.getSerializable(
                "type"
            ) as? ActivityType?
        }
        val appName = extras?.getString("bundle") ?: DownloadReport.bundle

        Log.i("TYPE OF ACTIVITY", "TYPE IS $type")
        if (type == ActivityType.StartExperience)
        {
            result = picoManager?.picoActivityService?.startActivity(
                appName,
            )

            Log.i("ACTIVITY", "Result: $result")

        }
        else if (type == ActivityType.UninstallExperience)
        {
            result = picoManager?.picoControlAPP?.silentUninstall(
                appName,
            )

            Log.i("ACTIVITY", "Result: $result")
        }
        if (result == null)
            return

        val payload = JsonBuilder.putExtras(
            JsonBuilder.extra("status", when
            {
                result == 0 -> JobState.Complete
                else -> JobState.Fail
            }),
            JsonBuilder.extra("typeOfJob", CommandType.Activity)
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
    suspend fun startDeploymentService(jobId: Long, extras: Bundle?, options: Bundle?): Boolean
    {
        deployment = DeploymentClass(
            extras,
            jobId,
            appContext
        )
        Log.i("DEPLOYMENT_SERVICE", "STARTING DEPLOYMENT")
        deployment?.start()
        return true
    }

    ////////////////////////////

    private fun onConnected()
    {
        //udpListener.stopDnsDiscovery()
    }
    private suspend fun onDisconnected()
    {
        if (ConnectionManager.startPicoConnection(picoManager))
        {
            commandManager = CommandManager(
                appContext,
                picoManager?.picoActivityService)
            compareConfig()
            val connectInfo = ConnectionManager.waitForServer(mDnsListener)
            if (connectInfo != null)
            {
                PersistentData.agentConfigurationWriter.updateClientData(
                    ipClient = picoManager?.picoInformationService?.getIpAddress(),
                    ipServer = connectInfo.serverIp,
                    portDownloads = connectInfo.serverPort,
                    portWebSocket = connectInfo.webSocketPort
                )

                ConnectionManager.startWebSocketService(
                    PersistentData.agentConfigurationReader.serverConnectionIp,
                    PersistentData.agentConfigurationReader.portWebSocket)
            }
        }
        else
            return
    }
    suspend fun compareConfig()
    {
        withContext(Dispatchers.IO)
        {
            val oldConfig = picoManager?.picoStore?.getPicoConfig()
            PersistentData.init(appContext, oldConfig)
            Log.e("PICO_MANAGER", "OLD MDM Configurations: ${picoManager?.picoStore?.getPicoConfig()}")
            val newConfig = PersistentData.picoConfigurationReader
            Log.e("PICO_MANAGER", "NEW MDM Configurations: ${newConfig.wifiSSID}")

            Log.i("PICO_MANAGER", "${picoManager?.picoInformationService?.getWifiStatus()}")
            Log.i("PICO_MANAGER", "${picoManager?.picoInformationService?.getPUIVersion()}")
            PersistentData.picoConfigurationWriter.updatePuiVersion(picoManager?.picoInformationService?.getPUIVersion())
            //Log.e("DEVICE OWNER", "New Device Owner: ${picoManager?.picoControlAPP?.setDeviceOwner()}")
            //Log.i("PICO_MANAGER", "Updating: ${picoManager?.picoControlAPP?.offlineUpdate()}")
            if (newConfig.configChange)
            {
                Log.e("PICO_CONFIGURATION", "Default Wi-Fi Connection: ${picoManager?.picoWifiService?.setDefaultWifiConnection(newConfig.wifiSSID, newConfig.wifiPassword)}")
                Log.e("PICO_CONFIGURATION", "Keep Wi-Fi On: ${picoManager?.picoWifiService?.setKeepWifi(newConfig.keepWifiOn)}")

                Log.e("PICO_CONFIGURATION", "Wireless Debug: ${picoManager?.picoDebugService?.setWirelessDebug(newConfig.wirelessDebug)}")
                Log.e("PICO_CONFIGURATION", "Usb Debug: ${picoManager?.picoDebugService?.setUsbDebug(newConfig.usbDebug)}")

                //Log.e("PICO_CONFIGURATION", "Fence Color: ${picoManager?.picoLbeService?.setFenceColor(newConfig.colorFence)}")
                //Log.e("PICO_CONFIGURATION", "Distance Sensitivity: ${picoManager?.picoLbeService?.setDistanceSensitivity(newConfig.distanceSensitivityPlayBoundary)}")
                if (newConfig.needBoot)
                {
                    Log.e("PICO_CONFIGURATION", "Sleep Delay: ${picoManager?.picoUtilitiesService?.setSleepDelay()}")
                    Log.e("PICO_CONFIGURATION", "Screen Off Delay ${picoManager?.picoUtilitiesService?.setScreenOffDelay()}")
                    Log.e("PICO_CONFIGURATION", "Auto Sleep: ${picoManager?.picoUtilitiesService?.setAutoSleep()}")
                    Log.e("PICO_CONFIGURATION", "Skip OOBE: ${picoManager?.picoUtilitiesService?.skipOOBE()}")

                    //Log.e("PICO_CONFIGURATION", "Home Gesture Disable: ${picoManager?.picoLbeService?.disableHomeGesture()}")
                    //Log.e("PICO_CONFIGURATION","Long Home Button Press Disable: ${picoManager?.picoLbeService?.disableLongHomePress()}")
                    //Log.e("PICO_CONFIGURATION", "Marker Detection: ${picoManager?.picoLbeService?.setMarkerDetection()}")
                    //Log.e("PICO_CONFIGURATION", "Marker First: ${picoManager?.picoLbeService?.setMarkerFirst()}")
                    //Log.e("PICO_CONFIGURATION", "Disable Boundary Pop Up Confirmation: ${picoManager?.picoLbeService?.disableBoundaryConfirmationPopUp()}")

                    val result = picoManager?.picoStore?.savePicoConfig(newConfig)
                    Log.e("PICO_MANAGER", "Device is going to reboot, config: ${picoManager?.picoStore?.getPicoConfig()}")
                    delay(10000.milliseconds)
                    picoManager?.picoUtilitiesService?.rebootPico()
                }
            }
            Log.e("PICO_CONFIGURATION", "Hand And Controller: ${picoManager?.picoLbeService?.setHandAndController(newConfig.handAndControllers)}")
            Log.e("PICO_CONFIGURATION", "Hand Tracking: ${picoManager?.picoLbeService?.setHandTracking(newConfig.handAndControllers)}")
            Log.e("PICO_CONFIGURATION", "Mixed interaction: ${picoManager?.picoLbeService?.setMixedInteraction(newConfig.handAndControllers)}")
            Log.e("PICO_MANAGER", "Device MDM Configurations: ${picoManager?.picoStore?.getPicoConfig()}")
            //Log.e("PICO_MANAGER", "Import of map: ${picoManager?.picoLbeService?.importMap()}")
            delay(5000.milliseconds)
        }
    }
    fun onDestroy()
    {
        picoManager?.picoDisconnection()
    }
    fun shutDown()
    {
        deployment?.shutdown()
        serviceScope.launch {
            deployment?.sendReport()
        }
    }
}