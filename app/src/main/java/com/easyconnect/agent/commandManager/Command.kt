package com.easyconnect.agent.commandManager

import com.easyconnect.agent.model.JobType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Command(
    var id: Long,
    var jobType: JobType,
    var extras: JsonObject ?= null,
    var options: JsonObject ?= null,
)