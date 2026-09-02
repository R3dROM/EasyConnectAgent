package com.easyconnect.agent.interfaces

import com.pvr.tobservice.enums.PBS_DeviceControlEnum
import com.pvr.tobservice.enums.PBS_ScreenOffDelayTimeEnum
import com.pvr.tobservice.enums.PBS_SleepDelayTimeEnum

interface IPicoUtilities {
    fun rebootPico()
    fun shutDownPico()
    fun resetTrackingLBE() : Int
    fun skipOOBE() : Int
    fun setScreenOffDelay()
    fun setSleepDelay()
    fun setAutoSleep() : Int
}