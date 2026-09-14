package com.easyconnect.agent.deployment.movefiles

import android.content.Context
import android.util.Log
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.utilities.DownloadReport
import com.easyconnect.agent.interfaces.IDeployProcess
import com.easyconnect.agent.model.MessageType
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import com.easyconnect.agent.model.DeploymentState
import com.easyconnect.agent.network.report.Report
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MoveFilesClass(
    private var appContext: Context,
    private var adbClient: Kadb?,
    private var bundle: String
): IDeployProcess {

    private var processState: Boolean = false
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
        val toPath = appContext.getExternalFilesDir(null)!!
            .parentFile!!
            .parentFile!!
            .absolutePath

        val clear = adbClient?.shell(
            command = "rm -rf $toPath/$bundle"
        )

        Log.i("MOVE ADB CLIENT", "cleaning result: $clear")

        Log.i("MOVE","TO PATH: $toPath")
        val moveResponse = adbClient?.shell(
            command = "mv -f $fromPath $toPath"
        )
        Log.i("MOVE ADB CLIENT", "result: $moveResponse")
        val result = moveResponse?.output?.let {
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
        val moveResponse = adbClient?.shell(
            command = "mv -f ${fromPath.absolutePath} $target"
        )
        val result = moveResponse?.output?.let {
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
        return withContext(Dispatchers.IO)
        {
            val outputPattern = Regex("""Shell response \((\d+)\)""")
            Log.i("MOVE", "STARTING MOVE")
            val bundleResponse = moveBundle(outputPattern)
            val apkResponse = moveApk(outputPattern)
            bundleResponse && apkResponse
        }
    }

    override fun shutdown() {

    }

    override suspend fun sendReport() {
        val payload = buildJsonObject {
            put("status", DeploymentState.Move.name)
        }
        val report = Report(
            id = PersistentData.agentConfigurationReader.ip,
            type = MessageType.Deployment,
            payload = payload
        )
    }
}