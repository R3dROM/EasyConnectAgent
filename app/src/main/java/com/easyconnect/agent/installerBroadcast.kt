package com.easyconnect.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageInstallReceiver(): BroadcastReceiver()
{
    init {
        Log.e("InstallREceiver","INICIADO INSTALLER RECEIVER")
    }
    override fun onReceive(context: Context, intent: Intent) {

        Log.e("InstallReceiver", "ACTION: ${intent.action}")
        Log.e("InstallReceiver", "DATA: ${intent.data}")
        Log.e("InstallReceiver", "EXTRAS: ${intent.extras}")

        val packageName = intent.data?.schemeSpecificPart

        Log.e("InstallReceiver", "PACKAGE: $packageName")
    }
}