package com.easyconnect.agent.pico

import android.util.Log
import com.easyconnect.agent.interfaces.IPicoWifi
import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemFunctionSwitchEnum
import com.pvr.tobservice.interfaces.IBoolCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoWifi(
    private val service : IToBServiceProxy
) : IPicoWifi {
    private val booleanCallback = object : IBoolCallback.Stub()
    {
        override fun callBack(p0: Boolean) {
            Log.i(
                "PICOBinder",
                "Callback: $p0"
            )
        }
    }
    // WIFI CONFIGURATION
    override fun setDefaultWifiConnection(ssid: String, pwd: String) =
        service.pbsControlSetAutoConnectWIFI(
            ssid,
            pwd,
            0,
            booleanCallback)
    override fun setKeepWifi(enable: Boolean) =
        run {
            service.pbsSwitchSystemFunction(
                PBS_SystemFunctionSwitchEnum.SFS_POWER_CTRL_WIFI_ENABLE,
                when(enable)
                {
                    true -> PBS_SwitchEnum.S_ON
                    else -> PBS_SwitchEnum.S_OFF
                } as PBS_SwitchEnum?,
                0
            )
        }
}