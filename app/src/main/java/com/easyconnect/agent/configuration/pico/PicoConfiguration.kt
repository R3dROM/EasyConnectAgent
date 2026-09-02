package com.easyconnect.agent.configuration.pico

import com.easyconnect.agent.interfaces.IPicoConfigurationReader
import com.easyconnect.agent.interfaces.IPicoConfigurationWriter

object PicoConfiguration : IPicoConfigurationWriter, IPicoConfigurationReader {
    override var wifiSSID: String = ""
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var wifiPassword: String = ""
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var keepWifiOn: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var wirelessDebug: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var usbDebug: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }

    override var homeGesture: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var calibrateGesture: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var handAndControllers: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }

    override var modeLBE: Boolean = false
        private set(value)
        {
            if (field != value)
                field = value
        }
    override var longHomePressButton: Boolean = false
        private set(value)
        {
            if (field != value)
                field = value
        }
    override var temporaryPlayBoundary: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var boundaryConfirmationPopup: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var useMarkersFirstPositionRecovery: Boolean = false
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var distanceSensitivityPlayBoundary: Int = 0
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var colorFence: Triple<Int, Int, Int> = Triple(0,0,0)
        private set(value)
        {
            if (field != value)
            {
                field = value
                needBoot = true
            }
        }
    override var needBoot = false
//        get()
//        {
//            val swap = field
//            needBoot = false
//            return swap
//        }

    override var configChange = false
    override fun updateClientData(
        wifiSSID: String?,
        wifiPassword: String?,
        keepWifiOn: Boolean?,
        wirelessDebug: Boolean?,
        usbDebug: Boolean?,
        homeGesture: Boolean?,
        calibrateGesture: Boolean?,
        handAndControllers: Boolean?,
        modeLBE: Boolean?,
        longHomePressButton: Boolean?,
        temporaryPlayBoundary: Boolean?,
        boundaryConfirmationPopup: Boolean?,
        useMarkersFirstPositionRecovery: Boolean?,
        distanceSensitivityPlayBoundary: Int?,
        colorFence: Triple<Int, Int, Int>?,
        configChange: Boolean
    ) {
        wifiSSID?.let { this.wifiSSID = it }
        wifiPassword?.let { this.wifiPassword = it }
        wirelessDebug?.let { this.wirelessDebug = it }
        modeLBE?.let { this.modeLBE = it }
        usbDebug?.let { this.usbDebug = it }
        boundaryConfirmationPopup?.let { this.boundaryConfirmationPopup = it }
        calibrateGesture?.let { this.calibrateGesture = it }
        homeGesture?.let { this.homeGesture = it }
        handAndControllers?.let { this.handAndControllers = it }
        useMarkersFirstPositionRecovery?.let { this.useMarkersFirstPositionRecovery = it }
        keepWifiOn?.let { this.keepWifiOn = it }
        longHomePressButton?.let { this.longHomePressButton = it }
        temporaryPlayBoundary?.let { this.temporaryPlayBoundary = it }
        distanceSensitivityPlayBoundary?.let { this.distanceSensitivityPlayBoundary = it }
        colorFence?.let { this.colorFence = it }

        this.configChange = configChange
    }
}