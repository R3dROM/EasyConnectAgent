package com.easyconnect.agent.pico

import android.util.Log
import com.easyconnect.agent.interfaces.IPicoWifi
import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemFunctionSwitchEnum
import com.pvr.tobservice.interfaces.IBoolCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PicoWifi(
    private val service : IToBServiceProxy
) : IPicoWifi {
    private val booleanCallback = object : IBoolCallback.Stub()
    {
        override fun callBack(p0: Boolean) {
            Log.i(
                "PICOBinder",
                "Callback WIFI: $p0"
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
    override suspend fun setKeepWifi(enable: Boolean) =
        suspendCancellableCoroutine { continuation ->
            service.pbsSwitchSystemFunction(
                PBS_SystemFunctionSwitchEnum.SFS_POWER_CTRL_WIFI_ENABLE.index,
                when(enable)
                {
                    true -> PBS_SwitchEnum.S_ON.index
                    else -> PBS_SwitchEnum.S_OFF.index
                },
                { result ->

                    Log.i(
                        "PICO WIFI",
                        "Keep Wifi On result: $result"
                    )

                    when (result) {
                        0 -> continuation.resume(true)
                        1, 2 -> continuation.resume(false)
                        else -> continuation.resume(false)
                    }
                },
                0
            )
        }
}