package com.alad1nks.oquturbo.feature.home.ui

import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import org.jetbrains.compose.resources.StringResource

internal sealed interface PersonalResultMetadata {
    data object None : PersonalResultMetadata

    data class Custom(val length: Int, val digits: String) : PersonalResultMetadata

    data class Language(val resource: StringResource) : PersonalResultMetadata

    data object UnknownSettings : PersonalResultMetadata

    data object UnknownLanguage : PersonalResultMetadata

    data object UnknownVariant : PersonalResultMetadata
}

internal fun GameSeriesKey.personalResultMetadata(): PersonalResultMetadata =
    when (mode) {
        GameModeId.NumberSprintCustom -> variantId?.customMetadata() ?: PersonalResultMetadata.UnknownSettings
        GameModeId.WordFlowContext ->
            variantId?.wordFlowLanguageResource()?.let(PersonalResultMetadata::Language)
                ?: PersonalResultMetadata.UnknownLanguage
        else -> if (variantId == null) PersonalResultMetadata.None else PersonalResultMetadata.UnknownVariant
    }

private fun String.customMetadata(): PersonalResultMetadata.Custom? {
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
    return PersonalResultMetadata.Custom(length, digits)
}

internal fun Int.signedResultChange(): String =
    when {
        this > 0 -> "+$this"
        this < 0 -> "−${-toLong()}"
        else -> "0"
    }
