package com.easyconnect.agent.interfaces

interface IPicoInformation {
    fun getIpAddress() : String
    fun getWifiStatus() : String
    fun getPUIVersion() : String
    fun getLBEMode()
}