package com.easyconnect.agent.pico

import android.util.Log
import com.easyconnect.agent.interfaces.IPicoInformation
import com.pvr.tobservice.enums.PBS_SystemInfoEnum
import com.pvr.tobservice.interfaces.IStringCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoInformation(
    private val service : IToBServiceProxy
): IPicoInformation {
    private val lbeCallback = object : IStringCallback.Stub()
    {
        override fun callback(p0: String?) {
            Log.i("PICOBinder", "Callback LBE Mode: $p0")
        }
    }

    override fun getIpAddress() : String =
        service.pbsStateGetDeviceInfo(
        PBS_SystemInfoEnum.DEVICE_IP,
        0
    )
    override fun getWifiStatus() : String =
        service.pbsStateGetDeviceInfo(
        PBS_SystemInfoEnum.DEVICE_WIFI_STATUS,
        0
    )
    override fun getPUIVersion() : String =
        service.pbsStateGetDeviceInfo(
        PBS_SystemInfoEnum.PUI_VERSION,
        0
    )
    override fun getLBEMode() =
        service.pbsGetSwitchLargeSpaceStatus(
            lbeCallback,
            0)
}