package com.easyconnect.agent.pico

import com.easyconnect.agent.interfaces.IPicoDebug
import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemFunctionSwitchEnum
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoDebug(
    private val service : IToBServiceProxy
) : IPicoDebug {
    // DEBUG
    override fun setUsbDebug(enable: Boolean) =
        run {
            service.pbsSwitchSystemFunction(
                PBS_SystemFunctionSwitchEnum.SFS_USB,
                when (enable) {
                    true -> PBS_SwitchEnum.S_ON
                    else -> PBS_SwitchEnum.S_OFF
                } as PBS_SwitchEnum?, 0)
        }
    override fun setWirelessDebug(enable: Boolean) =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_WIRELESS_USB_ADB,
            when (enable) {
                true -> PBS_SwitchEnum.S_ON
                else -> PBS_SwitchEnum.S_OFF } as PBS_SwitchEnum?,
            0)

}