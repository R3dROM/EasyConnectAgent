package com.easyconnect.agent.interfaces

import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemFunctionSwitchEnum

interface IPicoWifi {
    fun setDefaultWifiConnection(ssid: String, pwd: String)
    fun setKeepWifi(enable: Boolean)
}