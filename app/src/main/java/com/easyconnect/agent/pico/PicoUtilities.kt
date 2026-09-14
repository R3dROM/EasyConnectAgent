package com.easyconnect.agent.pico

import android.util.Log
import com.easyconnect.agent.interfaces.IPicoUtilities
import com.pvr.tobservice.Constants
import com.pvr.tobservice.enums.PBS_DeviceControlEnum
import com.pvr.tobservice.enums.PBS_ScreenOffDelayTimeEnum
import com.pvr.tobservice.enums.PBS_SleepDelayTimeEnum
import com.pvr.tobservice.interfaces.IIntCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoUtilities(
    private val service : IToBServiceProxy
) : IPicoUtilities {
    private val flagsOOBE =
        Constants.INIT_SETTING_QUICK_SETTING or
                Constants.INIT_SETTING_WIFI_SETTING or
                Constants.INIT_SETTING_HANDLE_CONNECTION_TEACHING or
                Constants.INIT_SETTING_SELECT_COUNTRY or
                Constants.INIT_SETTING_SELECT_LANGUAGE or
                Constants.INIT_SETTING_TRIGGER_KEY_TEACHING
    private val intCallback = object : IIntCallback.Stub()
    {
        override fun callback(p0: Int) {
            Log.i(
                "PICOBinder",
                "Callback: $p0"
            )
        }
    }
    // UTILITIES
    override fun rebootPico() = service.pbsControlSetDeviceAction(
        PBS_DeviceControlEnum.DEVICE_CONTROL_REBOOT,
        intCallback
    )
    override fun shutDownPico() = service.pbsControlSetDeviceAction(
        PBS_DeviceControlEnum.DEVICE_CONTROL_SHUTDOWN,
        intCallback
    )
    override fun resetTrackingLBE() : Int = service.resetTracking()
    override fun skipOOBE() : Int =
        service.pbsSetSkipInitSettingPage(flagsOOBE, 0)
    override fun setScreenOffDelay() = service.pbsPropertySetScreenOffDelay(
        PBS_ScreenOffDelayTimeEnum.THIRTY,
        intCallback
    )
    override fun setSleepDelay() = service.pbsPropertySetSleepDelay(
        PBS_SleepDelayTimeEnum.ONE_THOUSAND_AND_EIGHT_HUNDRED
    )
    override fun setAutoSleep() : Int = service.setSystemAutoSleepTime(
        PBS_SleepDelayTimeEnum.NEVER
    )
}