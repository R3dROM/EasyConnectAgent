package com.easyconnect.agent.commandManager

import com.easyconnect.agent.model.CommandType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Command(
    var id: Long,
    var commandType: CommandType,
    var extras: JsonObject
)