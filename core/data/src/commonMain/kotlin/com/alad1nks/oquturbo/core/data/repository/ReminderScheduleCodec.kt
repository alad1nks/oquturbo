package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal fun decodeReminderSchedule(value: String?): ReminderSchedule? {
    if (value.isNullOrBlank()) return null
    val root = Json.parseToJsonElement(value) as? JsonObject ?: error("Invalid reminder schedule")

    fun integer(fields: JsonObject, key: String): Int {
        val number = fields[key] as? JsonPrimitive ?: error("Missing $key")
        check(!number.isString)
        return number.intOrNull ?: error("Invalid $key")
    }

    fun text(fields: JsonObject, key: String): String {
        val text = fields[key] as? JsonPrimitive ?: error("Missing $key")
        check(text.isString)
        return text.content
    }
    check(integer(root, "version") == 1)
    val content = root["content"] as? JsonObject ?: error("Missing content")
    return ReminderSchedule(
        integer(root, "minutesOfDay"),
        ReminderContent(
            text(content, "languageCode"),
            text(content, "title"),
            text(content, "body"),
        ),
    )
}

internal fun ReminderSchedule.encodeReminder(): String =
    buildJsonObject {
        put("version", 1)
        put("minutesOfDay", minutesOfDay)
        put(
            "content",
            buildJsonObject {
                put("languageCode", content.languageCode)
                put("title", content.title)
                put("body", content.body)
            },
        )
    }.toString()
