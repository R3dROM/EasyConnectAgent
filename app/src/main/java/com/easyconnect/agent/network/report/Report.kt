package com.easyconnect.agent.network.report

import com.easyconnect.agent.model.MessageType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

data class Report(
    private val id: String,
    private val type: MessageType?,
    private var timestamp: Long ?= null,
    private var jobId: Long ?= null,
    private var payload: JsonObject = buildJsonObject {  }
)
{
    fun toJson(): JsonObject =
        buildJsonObject {
            put("id", Json.encodeToJsonElement(id))
            type?.let {
                put("type", Json.encodeToJsonElement(it))
            }

            timestamp?.let {
                put("timestamp", Json.encodeToJsonElement(it))
            }

            jobId?.let {
                put("jobId", Json.encodeToJsonElement(it))
            }

            put("payload", Json.encodeToJsonElement(payload))
        }
}