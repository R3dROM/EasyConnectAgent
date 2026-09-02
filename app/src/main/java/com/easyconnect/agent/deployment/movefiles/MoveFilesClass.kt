package com.easyconnect.agent.deployment.movefiles

import android.content.Context
import android.util.Log
import com.easyconnect.agent.deployment.download.DownloadReport
import com.easyconnect.agent.interfaces.IDeployProcess
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class MoveFilesClass(
    private var appContext: Context,
    private var adbClient: Kadb?,
    private var bundle: String
): IDeployProcess {
    suspend fun moveBundle(pattern: Regex) : Boolean
    {
        Log.i("MOVE","STARTING MOVING BUNDLE")

        val fromPath = File(
            appContext.getExternalFilesDir(null),
            bundle
        )
        if (!fromPath.exists())
            return false
        Log.i("MOVE","FROM PATH: $fromPath")

        val toPath = File(
            appContext.getExternalFilesDir(null)!!
                .parentFile!!
                .parentFile!!,
            bundle
        ).absolutePath

        Log.i("MOVE","TO PATH: $toPath")
        val output = adbClient?.shell(
            command = "mv -f $fromPath $toPath"
        )
        Log.i("MOVE ADB CLIENT", "result: $output")
        val result = output?.output?.let {
            pattern.find(it)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }
        delay(3000.milliseconds)
        return !(result == null || result != 0)
    }
    suspend fun moveApk(pattern: Regex): Boolean
    {
        Log.i("MOVE","STARTING MOVING APK")
        val fromPath = File(
            appContext.getExternalFilesDir(null)!!
                .parentFile!!
                .parentFile!!,
            "$bundle/apk/${DownloadReport.apkName}"
        )
        if (!fromPath.exists())
            return false

        val target = "/data/local/tmp/target.apk"
        val output = adbClient?.shell(
            command = "mv -f ${fromPath.absolutePath} $target"
        )
        val result = output?.output?.let {
            pattern.find(it)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }
        if (result == 0)
        {
            adbClient?.shell(
                command = "rm -f ${fromPath.absolutePath}"
            )
        }
        delay(3000.milliseconds)
        return !(result == null || result != 0)
    }
    override suspend fun start(): Boolean {
        withContext(Dispatchers.IO)
        {
            val outputPattern = Regex("""Shell response \((\d+)\)""")
            Log.i("MOVE", "STARTING MOVE")
            moveBundle(outputPattern)
            moveApk(outputPattern)
            return@withContext
        }
        return true
    }

    override fun shutdown() {

    }

    override suspend fun sendReport(report: DownloadReport) {

    }
}