package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.DayHistory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Kotlin null means absent property; JsonNull is present, invalid metadata. */
internal fun decodeDayHistory(element: JsonElement?): DayHistory? {
    if (element == null) return null
    val fields = element as? JsonObject ?: error("Stored day history must be an object")
    check(fields["version"].historyLong() == 1L) { "Unsupported day history version" }
    val start = fields["trackingStartedEpochDay"].historyDay()
    val days = fields["completedEpochDays"] as? JsonArray ?: error("Stored history days must be an array")
    return DayHistory(start, days.map { it.historyDay() })
}

internal fun DayHistory.encodeHistory(): JsonObject =
    JsonObject(
        mapOf(
            "version" to JsonPrimitive(1),
            "trackingStartedEpochDay" to JsonPrimitive(trackingStartedEpochDay),
            "completedEpochDays" to JsonArray(completedEpochDays.sorted().map(::JsonPrimitive)),
        ),
    )

private fun JsonElement?.historyLong(): Long {
    val primitive = this as? JsonPrimitive ?: error("Stored history date/version must be an integer")
    check(!primitive.isString) { "Stored history date/version must not be a string" }
    return primitive.longOrNull ?: error("Stored history date/version must be an integer")
}

// Full range of the existing Long epoch-millisecond writer; do not narrow to Int epoch days.
private fun JsonElement?.historyDay(): Long =
    historyLong().also {
        check(it in Long.MIN_VALUE / 86_400_000L..Long.MAX_VALUE / 86_400_000L) {
            "Stored history date is outside the epoch-millisecond writer range"
        }
    }
