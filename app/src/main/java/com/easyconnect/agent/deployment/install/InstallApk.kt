package com.easyconnect.agent.deployment.install

import android.content.Context
import android.util.Log
import com.easyconnect.agent.deployment.download.DownloadReport
import com.easyconnect.agent.interfaces.IDeployProcess
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstallApk(
    private val appContext: Context,
    private val adbClient: Kadb?
): IDeployProcess {
    override suspend fun start(): Boolean {
        withContext(Dispatchers.IO)
        {
            Log.i("INSTALLING", "Supports cmd : ${adbClient?.supportsFeature("cmd")}")
            val target = "/data/local/tmp/target.apk"
            val inst = adbClient?.shell(
                command = "cmd package install -r -g $target"
            )
            Log.i("INSTALLING", "RESULT: $inst")
            if (inst?.output?.contains("Success") == true)
                adbClient.shell(
                    command = "rm -rf $target"
                )
        }
        return true
    }

    override fun shutdown() {

    }

    override suspend fun sendReport(report: DownloadReport) {

    }
}