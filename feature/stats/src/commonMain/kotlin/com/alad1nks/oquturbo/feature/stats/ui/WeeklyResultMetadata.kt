package com.alad1nks.oquturbo.feature.stats.ui

import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource

internal sealed interface WeeklyResultMetadata {
    data object None : WeeklyResultMetadata

    data class Custom(val length: Int, val digits: String) : WeeklyResultMetadata

    data class Language(val resource: StringResource) : WeeklyResultMetadata

    data object UnknownSettings : WeeklyResultMetadata

    data object UnknownLanguage : WeeklyResultMetadata

    data object UnknownVariant : WeeklyResultMetadata
}

internal fun GameSeriesKey.weeklyResultMetadata(): WeeklyResultMetadata =
    when (mode) {
        GameModeId.NumberSprintCustom -> variantId?.customMetadata() ?: WeeklyResultMetadata.UnknownSettings
        GameModeId.WordFlowContext ->
            variantId?.wordFlowLanguageResource()?.let(WeeklyResultMetadata::Language)
                ?: WeeklyResultMetadata.UnknownLanguage
        else -> if (variantId == null) WeeklyResultMetadata.None else WeeklyResultMetadata.UnknownVariant
    }

private fun String.customMetadata(): WeeklyResultMetadata.Custom? {
    val parts = split(';')
    if (parts.size != 2) return null
    val values = mutableMapOf<String, String>()
    for (part in parts) {
        val pair = part.split(':')
        if (pair.size != 2 || pair[0] !in setOf("length", "digits") || values.put(pair[0], pair[1]) != null) return null
    }
    val length = values["length"]?.toIntOrNull()?.takeIf { it > 0 } ?: return null
    val digits = values["digits"] ?: return null
    if (digits.length < 2 || digits.any { it !in '0'..'9' } || digits.toSet().size != digits.length) return null
    return WeeklyResultMetadata.Custom(length, digits)
}

internal fun Int.signedWeeklyChange(): String =
    when {
        this > 0 -> "+$this"
        this < 0 -> "−${-toLong()}"
        else -> "0"
    }

private fun String.wordFlowLanguageResource(): StringResource? =
    when (this) {
        "en" -> AppResource.String.language_english
        "ru" -> AppResource.String.language_russian
        "kk" -> AppResource.String.language_kazakh
        else -> null
    }
