package com.vibeon.music.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val TubeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    coerceInputValues = true
}

fun JsonElement?.navigate(vararg path: String): JsonElement? =
    path.fold(this) { acc, key -> acc?.jsonObjectOrNull?.get(key) }

fun JsonElement?.obj(vararg path: String): JsonObject? =
    navigate(*path) as? JsonObject

fun JsonElement?.arr(vararg path: String): JsonArray? =
    navigate(*path) as? JsonArray

fun JsonElement?.str(vararg path: String): String? =
    (navigate(*path) as? JsonPrimitive)?.contentOrNull

fun JsonElement?.int(vararg path: String): Long? =
    (navigate(*path) as? JsonPrimitive)?.content?.toLongOrNull()

fun JsonElement?.bool(vararg path: String): Boolean? =
    (navigate(*path) as? JsonPrimitive)?.let { p ->
        p.booleanOrNull ?: p.content.toBooleanStrictOrNull()
    }

fun JsonObject.deepFind(key: String, depth: Int = 10): JsonElement? {
    if (depth <= 0) return null
    get(key)?.let { return it }
    for (value in values) {
        when (value) {
            is JsonObject -> value.deepFind(key, depth - 1)?.let { return it }
            is JsonArray -> for (item in value) {
                if (item is JsonObject) item.deepFind(key, depth - 1)?.let { return it }
            }
            else -> Unit
        }
    }
    return null
}

fun JsonElement.deepFind(key: String, depth: Int = 10): JsonElement? = when (this) {
    is JsonObject -> this.deepFind(key, depth)
    is JsonArray -> forEachNotNull { it.deepFind(key, depth - 1) }
    else -> null
}

private inline fun JsonArray.forEachNotNull(block: (JsonElement) -> JsonElement?): JsonElement? {
    for (item in this) {
        block(item)?.let { return it }
    }
    return null
}

val JsonElement?.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

val JsonElement?.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

fun JsonElement?.runsText(location: JsonElement? = null): String {
    val runs = (location ?: this)?.navigate("runs") as? JsonArray ?: return ""
    return runs.mapNotNull { run ->
        (run as? JsonObject)?.get("text") as? JsonPrimitive ?: return@mapNotNull null
    }.let { primitives ->
        primitives.joinToString("") { it.contentOrNull ?: "" }
    }
}

fun JsonElement?.listText(vararg path: String): String {
    val item = navigate(*path) ?: return ""
    val runs = (item.navigate("runs") as? JsonArray
        ?: item.navigate("text") as? JsonArray) ?: return ""
    return runs.mapNotNull { run ->
        (run as? JsonObject)?.get("text") as? JsonPrimitive
    }.joinToString("") { it.contentOrNull ?: "" }
}