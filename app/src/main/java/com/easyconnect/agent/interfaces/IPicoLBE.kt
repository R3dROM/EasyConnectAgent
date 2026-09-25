package com.easyconnect.agent.interfaces

interface IPicoLBE {
    fun disableBoundaryConfirmationPopUp()
    fun setMarkerFirst()
    fun setMarkerDetection()
    fun setDistanceSensitivity(distance: Int) : Int
    fun setFenceColor(color: Triple<Int, Int, Int>) : Int
    fun setLBEMode(enable: Boolean)
    suspend fun setMixedInteraction(enable: Boolean): Boolean
    suspend fun setHandTracking(enable: Boolean): Boolean
    suspend fun setHandAndController(enable: Boolean): Boolean
    fun disableHomeGesture()
    fun disableLongHomePress()
    fun importMap()
    fun isMapInEffect()
    fun importMapByPath(path: String)
}