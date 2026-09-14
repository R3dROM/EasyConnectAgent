package com.easyconnect.agent.utilities

import com.easyconnect.agent.model.DeviceStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

object JsonBuilder {
    fun putExtras(vararg args: Pair<String, JsonElement>): JsonObject
    {
        return buildJsonObject {
            args.forEach { (key, value) ->
                put(key, value)
            }
        }
    }
    inline fun <reified T> extra(key: String, value: T): Pair<String, JsonElement> =
        key to Json.encodeToJsonElement(value)
    private inline fun <reified T> getExtra(payload: JsonObject, key: String) : T?
    {
        val value = payload[key] ?: return null
        return Json.decodeFromJsonElement<T>(value)
    }
    private inline fun <reified T> getExtraFromElement(element: JsonElement) : T
    {
        return Json.decodeFromJsonElement<T>(element)
    }

    fun getString(payload: JsonObject, key: String): String? = getExtra<String>(payload, key)
    fun getStringFromElement(payload: JsonElement): String = getExtraFromElement<String>(payload)
    fun getInt(payload: JsonObject, key: String): Int? = getExtra<Int>(payload, key)
    fun getIntFromElement(payload: JsonElement): Int = getExtraFromElement<Int>(payload)
    fun getLong(payload: JsonObject, key: String): Long? = getExtra<Long>(payload, key)
    fun getLongFromElement(payload: JsonElement): Long = getExtraFromElement<Long>(payload)
    fun getStatus(payload: JsonObject, key: String): DeviceStatus? = getExtra<DeviceStatus>(payload, key)
    fun getStatusFromElement(payload: JsonElement): DeviceStatus = getExtraFromElement<DeviceStatus>(payload)

    fun getBooleanFromElement(payload: JsonElement): Boolean = getExtraFromElement<Boolean>(payload)
}