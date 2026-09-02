package com.easyconnect.agent.interfaces

interface IPicoLBE {
    fun disableBoundaryConfirmationPopUp()
    fun setMarkerFirst()
    fun setMarkerDetection()
    fun setDistanceSensitivity(distance: Int) : Int
    fun setFenceColor(color: Triple<Int, Int, Int>) : Int
    fun setLBEMode(enable: Boolean)
    fun setHandTracking(enable: Boolean)
    fun setHandAndController(enable: Boolean)
    fun disableHomeGesture()
    fun disableLongHomePress()
    fun importMap()
    fun isMapInEffect()
    fun importMapByPath(path: String)
}