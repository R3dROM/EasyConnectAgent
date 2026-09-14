package com.easyconnect.agent.interfaces

import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemFunctionSwitchEnum

interface IPicoDebug {
    fun setUsbDebug(enable: Boolean)
    fun setWirelessDebug(enable: Boolean)
}