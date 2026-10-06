package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

internal fun decodeWeeklyFocus(value: String?): WeeklyFocus {
    if (value.isNullOrBlank()) return WeeklyFocus()
    val fields = Json.parseToJsonElement(value) as? JsonObject ?: error("Focus payload must be an object")
    check(fields["version"].focusLong() == 1L) { "Unsupported focus version" }
    val selection = fields["selection"] ?: error("Focus selection is required")
    if (selection == JsonNull) return WeeklyFocus()
    val settings = selection as? JsonObject ?: error("Focus selection must be an object or null")
    val kind = settings["kind"] as? JsonPrimitive ?: error("Focus kind is required")
    check(kind.isString && kind.content == "number_sprint_classic") { "Unsupported focus kind" }
    return WeeklyFocus(
        WeeklyFocusSelection(settings["startEpochDay"].focusLong(), settings["endExclusiveEpochDay"].focusLong()),
    )
}

internal fun WeeklyFocus.encodeFocus(): String =
    JsonObject(
        mapOf(
            "version" to JsonPrimitive(1),
            "selection" to (
                selection?.let {
                    JsonObject(
                        mapOf(
                            "kind" to JsonPrimitive("number_sprint_classic"),
                            "startEpochDay" to JsonPrimitive(it.startEpochDay),
                            "endExclusiveEpochDay" to JsonPrimitive(it.endExclusiveEpochDay),
                        ),
                    )
                } ?: JsonNull
            ),
        ),
    ).toString()

private fun JsonElement?.focusLong(): Long {
    val primitive = this as? JsonPrimitive ?: error("Focus date/version must be an integer")
    check(!primitive.isString) { "Focus date/version must not be a string" }
    return primitive.longOrNull ?: error("Focus date/version must be an integer")
}
