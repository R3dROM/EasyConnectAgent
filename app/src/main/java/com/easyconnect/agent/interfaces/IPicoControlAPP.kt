package com.easyconnect.agent.interfaces

import com.pvr.tobservice.enums.PBS_PackageControlEnum

interface IPicoControlAPP {
    fun silentInstall(path: String)
    fun silentUninstall(packageName: String)
    suspend fun grantPermissions()
    suspend fun requestManageStorage() : Boolean
    fun setDeviceOwner(): Boolean
}