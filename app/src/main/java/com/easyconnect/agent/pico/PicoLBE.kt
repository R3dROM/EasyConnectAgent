package com.easyconnect.agent.pico

import android.annotation.SuppressLint
import android.util.Log
import com.easyconnect.agent.interfaces.IPicoLBE
import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemFunctionSwitchEnum
import com.pvr.tobservice.interfaces.IBoolCallback
import com.pvr.tobservice.interfaces.IIntCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoLBE(
    private val service : IToBServiceProxy
) : IPicoLBE {
    private val intCallback = object : IIntCallback.Stub()
    {
        override fun callback(p0: Int) {
            Log.i(
                "PICOBinder",
                "Callback: $p0"
            )
        }
    }
    private val booleanCallback = object : IBoolCallback.Stub()
    {
        override fun callBack(p0: Boolean) {
            Log.i(
                "PICOBinder",
                "Callback: $p0"
            )
        }
    }
    private val lbeEnableCallback = object : IBoolCallback.Stub()
    {
        override fun callBack(p0: Boolean) {
            if (p0)
            {
                Log.i("PICO_BINDER", "LBE MODE ENABLE")
            }
            else
            {
                Log.i("PICO_BINDER", "LBE MODE FAIL TO ENABLE")
            }
        }
    }
    private val mapCheckCallback = object : IIntCallback.Stub()
    {
        override fun callback(p0: Int) {
            when (p0) {
                0 -> Log.i("PICO_BINDER", "Map already in use")
                1 -> {
                    Log.i("PICO_BINDER", "Map is not in use, importing map")
                    importMap()
                }
                else -> Log.i("PICO_BINDER", "Fail to check map")
            }
        }
    }
    // LBE CONFIGURATION
    override fun disableBoundaryConfirmationPopUp() =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_BOUNDARY_CONFIRMATION_SCREEN,
            PBS_SwitchEnum.S_OFF,
            0
        )
    override fun setMarkerFirst() =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_RETRIEVE_MAP_BY_MARKER_FIRST,
            PBS_SwitchEnum.S_ON,
            0
        )
    override fun setMarkerDetection() =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_DETECT_MARKER,
            PBS_SwitchEnum.S_ON,
            0
        )
    override fun setDistanceSensitivity(distance: Int) :Int =
        service.setDistanceSensitivity(distance)
    override fun setFenceColor(color: Triple<Int, Int, Int>) : Int =
        service.setFenceColor(
            1,
            color.first,
            color.second,
            color.third,
            255
        )
    override fun setLBEMode(enable: Boolean) =
        service.pbsSwitchLargeSpaceScene(
            lbeEnableCallback,
            enable,
            0)
    override fun setHandTracking(enable: Boolean) =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_GESTURE_RECOGNITION,
            when(enable)
            {
                true -> PBS_SwitchEnum.S_ON
                else -> PBS_SwitchEnum.S_OFF
            },
            0
        )
    override fun setHandAndController(enable: Boolean)
    {
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_MIXED_INTERACTION_MODE,
            when(enable)
            {
                true -> PBS_SwitchEnum.S_ON
                else -> PBS_SwitchEnum.S_OFF
            },
            0
        )
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_HEAD_HAND_INTERACTION,
            when(enable)
            {
                true -> PBS_SwitchEnum.S_ON
                else -> PBS_SwitchEnum.S_OFF
            },
            0
        )
    }
    override fun disableHomeGesture() =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_SYSTEM_HOME_GESTURE_DISABLE,
            PBS_SwitchEnum.S_ON,
            0
        )
    override fun disableLongHomePress() =
        service.pbsSwitchSystemFunction(
            PBS_SystemFunctionSwitchEnum.SFS_LONG_PRESS_HOME_TO_RECENTER,
            PBS_SwitchEnum.S_OFF,
            0
        )

    override fun importMap() =
        service.pbsImportMaps(
            booleanCallback,
            0)
    @SuppressLint("SdCardPath")
    override fun isMapInEffect() = service.isMapInEffect("/sdcard/maps/V2_map_8_18_9_15_40.zip",mapCheckCallback, 0)
    override fun importMapByPath(path: String) =
        service.importMapByPath(
            path,
            intCallback,
            0)
}