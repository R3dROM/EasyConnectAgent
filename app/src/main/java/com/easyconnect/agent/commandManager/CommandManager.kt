package com.easyconnect.agent.commandManager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.easyconnect.agent.configuration.ActivityConfiguration
import com.easyconnect.agent.configuration.agent.AgentConfiguration
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.data.queue.Communicator
import com.easyconnect.agent.interfaces.IPicoActivity
import com.easyconnect.agent.model.ActivityType
import com.easyconnect.agent.model.CommandType
import com.easyconnect.agent.model.JobState
import com.easyconnect.agent.model.MessageType
import com.easyconnect.agent.network.report.Report
import com.easyconnect.agent.utilities.JsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

class CommandManager(
    appContext: Context,
    private val intentStarter: IPicoActivity?
) {
    private var job: Job?= null
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
    init {
        job?.cancel()
        job = serviceScope.launch {
            while (isActive)
            {
                val command = Communicator.receiveCommand()
                val intentResult = createIntent(command)
                sendIntent(intentResult)

                if (command.commandType != CommandType.Connection)
                {
                    val payload = JsonBuilder.putExtras(
                        JsonBuilder.extra("status", JobState.Executing),
                        JsonBuilder.extra("typeOfJob", command.commandType)
                    )
                    val report = Report(
                        id = PersistentData.agentConfigurationReader.serialNumber,
                        payload = payload,
                        type = MessageType.Acknowledge,
                        jobId = command.id
                    )
                    Communicator.publishReport(report)
                }
            }
        }
    }
    fun createIntent(command: Command): Intent
    {
        val type = command.commandType
        val className = when (type) {
            CommandType.Connection -> AgentConfiguration.WEBSOCKET_CLASS_NAME
            else -> AgentConfiguration.EASY_AGENT_SERVICE_CLASS_NAME
        }
        val intent = Intent().apply {
            component = ComponentName(
                AgentConfiguration.PACKAGE_NAME,
                className
            )
            putExtra(ActivityConfiguration.TARGET,command.commandType)
            putExtra(ActivityConfiguration.JOB_ID, command.id)
            command.extras?.let { putExtra(ActivityConfiguration.EXTRAS, command.extras!!.toBundle()) }
            command.options?.let { putExtra(ActivityConfiguration.OPTIONS, command.options!!.toBundle())}
        }
        Log.i("COMMAND_MANAGER", "Intent created: $intent")
        return intent
    }

    fun sendIntent(intent: Intent) = intentStarter?.startService(intent)

    private fun JsonObject.toBundle(): Bundle {
        val bundle = Bundle()

        forEach { (key, element) ->
            when {
                key == "type" && element is JsonPrimitive && element.intOrNull != null -> {
                    try {
                        val value = JsonBuilder.getIntFromElement(element)
                        val activity = ActivityType.entries[value]
                        bundle.putSerializable(key, activity)
                    } catch (e: IllegalArgumentException) {
                        Log.w(
                            "OPTIONS",
                            "Unknown Activity value: ${element.content}"
                        )
                    }
                }
                element is JsonPrimitive && element.isString -> {
                    val string = JsonBuilder.getStringFromElement(element)
                    bundle.putString(key, string)
                }
                element is JsonPrimitive && element.booleanOrNull != null -> {
                    val boolean = JsonBuilder.getBooleanFromElement(element)
                    bundle.putBoolean(key, boolean)
                }
                element is JsonPrimitive && element.longOrNull != null -> {
                    val long = JsonBuilder.getLongFromElement(element)
                    bundle.putLong(key, long)
                }
                else -> {
                    println("null")
                }
            }
        }
        return bundle
    }
}