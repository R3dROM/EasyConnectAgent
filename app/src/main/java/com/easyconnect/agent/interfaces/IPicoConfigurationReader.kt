package com.easyconnect.agent.interfaces

interface IPicoConfigurationReader {
    val puiVersion: String
    val wifiSSID: String
    val wifiPassword: String
    val keepWifiOn: Boolean
    val wirelessDebug: Boolean
    val usbDebug: Boolean

    val homeGesture: Boolean
    val calibrateGesture: Boolean
    val handAndControllers: Boolean

    val modeLBE: Boolean
    val longHomePressButton: Boolean
    val temporaryPlayBoundary: Boolean
    val boundaryConfirmationPopup: Boolean
    val useMarkersFirstPositionRecovery: Boolean
    val distanceSensitivityPlayBoundary: Int
    val colorFence: Triple<Int, Int, Int>
    val needBoot: Boolean
    val configChange: Boolean
}