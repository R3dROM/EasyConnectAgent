package com.easyconnect.agent.interfaces

import com.pvr.tobservice.enums.PBS_PackageControlEnum

interface IPicoControlAPP {
    fun silentInstall(path: String)
    suspend fun silentUninstall(packageName: String): Int
    suspend fun grantPermissions()
    suspend fun requestManageStorage() : Boolean
    fun setDeviceOwner(): Boolean
    suspend fun offlineUpdate(): Int
}