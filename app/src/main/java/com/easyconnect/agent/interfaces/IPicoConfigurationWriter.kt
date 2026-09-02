package com.easyconnect.agent.interfaces

interface IPicoConfigurationWriter {
    fun updateClientData(
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
    )
}