package com.easyconnect.agent.pico

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.easyconnect.agent.admin.EasyDeviceAdminReceiver
import com.easyconnect.agent.core.EasyAgentClass
import com.easyconnect.agent.interfaces.IPicoControlAPP
import com.pvr.tobservice.ToBServiceHelper
import com.pvr.tobservice.enums.PBS_PackageControlEnum
import com.pvr.tobservice.interfaces.IIntCallback
import com.pvr.tobservice.interfaces.IToBService
import com.pvr.tobservice.interfaces.IToBServiceProxy
import com.pvr.tobservice.model.SystemPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.function.Consumer
import kotlin.coroutines.resume

class PicoControlAPP(
    private var appContext: Context,
    private val service: IToBService,
    private val proxy: IToBServiceProxy
) : IPicoControlAPP {
    private val controlAPPCallback = object : IIntCallback.Stub()
    {
        override fun callback(p0: Int) {
            Log.d("CONTROL_APP", "Control app: $p0")
        }
    }
    private val grantPermissionsCallback = Consumer<Int> { t ->
        when (t) {
            0 -> Log.e("PICO_PERMISSION", "Storage permission granted")
            1 -> Log.e("PICO_PERMISSION", "Failed to grant storage permission")
            2 -> Log.e("PICO_PERMISSION", "Permission verification failed")
            else -> Log.e(
                "PICO_PERMISSION",
                "Unknown result: $t"
            )
        }
    }
    override fun silentInstall(path: String) =
        service.pbsControlAPPManger(
            PBS_PackageControlEnum.PACKAGE_SILENCE_INSTALL,
            path,
            0,
            controlAPPCallback
        )
    override fun silentUninstall(packageName: String) =
        service.pbsControlAPPManger(
            PBS_PackageControlEnum.PACKAGE_SILENCE_UNINSTALL,
            packageName,
            0,
            controlAPPCallback
        )

    override suspend fun grantPermissions() {
//        service.pbsRequestSystemPermissionAsync(
//            SystemPermission.SYS_OPS_PERMISSION_INSTALL_APKS,
//            true,
//            grantPermissionsCallback
//        )
//        service.pbsRequestSystemPermissionAsync(
//            SystemPermission.SYS_RUNTIME_PERMISSION_STORAGE,
//            true,
//            grantPermissionsCallback
//        )
//        service.pbsRequestSystemPermissionAsync(
//            SystemPermission.SYS_OPS_PERMISSION_WRITE_SETTINGS,
//            true,
//            grantPermissionsCallback
//        )
    }
    override suspend fun requestManageStorage(): Boolean =
        suspendCancellableCoroutine { continuation ->

            proxy.pbsRequestSystemPermissionAsync(
                SystemPermission.SYS_OPS_PERMISSION_INSTALL_APKS,
                true
            ) { result ->

                Log.i(
                    "PICO_PERMISSION",
                    "INSTALL permission result: $result"
                )

                when (result) {
                    0 -> continuation.resume(true)
                    1, 2 -> continuation.resume(false)
                    else -> continuation.resume(false)
                }
            }
        }
    override fun setDeviceOwner(): Boolean {

        val admin  = ComponentName(
            appContext,
            EasyDeviceAdminReceiver::class.java
        )

        val result = proxy.setDeviceOwner(admin)


        Log.i("DEVICE_OWNER", "admin=$admin")
        Log.i("DEVICE_OWNER", "package=${admin.packageName}")
        Log.i("DEVICE_OWNER", "class=${admin.className}")
        Log.i(
            "PICO_DEVICE_OWNER",
            "setDeviceOwner result: $result"
        )

        return result == 0
    }
}