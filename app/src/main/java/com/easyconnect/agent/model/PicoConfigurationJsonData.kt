package com.easyconnect.agent.model

import kotlinx.serialization.Serializable

@Serializable
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
