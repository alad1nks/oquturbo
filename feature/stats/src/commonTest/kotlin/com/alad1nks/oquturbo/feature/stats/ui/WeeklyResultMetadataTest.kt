package com.alad1nks.oquturbo.feature.stats.ui

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.feature.stats.data.toStatsGame
import com.alad1nks.oquturbo.feature.stats.data.toStatsMode
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals

class WeeklyResultMetadataTest {
    @Test
    fun customCaptionKeepsPositiveStoredLengthAndLeadingZeroWithoutMenuLimit() {
        assertEquals(WeeklyResultMetadata.Custom(12, "013579"), custom("length:12;digits:013579"))
        assertEquals(WeeklyResultMetadata.Custom(12, "0123456789"), custom("digits:0123456789;length:12"))
    }

    @Test
    fun malformedCustomVariantsBecomeUnknownWithoutLeakingIdentifiers() {
        for (variant in listOf(
            null,
            "",
            "length:0;digits:01",
            "length:-1;digits:01",
            "length:x;digits:01",
            "length:99999999999;digits:01",
            "length:12;digits:",
            "length:12;digits:0",
            "length:12;digits:00",
            "length:12;digits:0121",
            "length:12;digits:0a",
            "length:12;digits:0 1",
            "length:12;digits:０１",
            "length:12;digits:01;extra:x",
            "length:12;length:13",
            "digits:01;digits:12",
            "length:12;digits:01;",
            "length:12;digits:01:2",
            "unknown:12;digits:01",
        )) assertEquals(WeeklyResultMetadata.UnknownSettings, custom(variant), variant)
    }

    @Test
    fun savedLanguageAndUnknownMetadataAreExplicit() {
        val key = GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "en")
        assertEquals(WeeklyResultMetadata.Language(AppResource.String.language_english), key.weeklyResultMetadata())
        assertEquals(
            WeeklyResultMetadata.Language(AppResource.String.language_russian),
            key.copy(variantId = "ru").weeklyResultMetadata(),
        )
        assertEquals(
            WeeklyResultMetadata.Language(AppResource.String.language_kazakh),
            key.copy(variantId = "kk").weeklyResultMetadata(),
        )
        for (variant in listOf(null, "de", "EN", "")) {
            assertEquals(WeeklyResultMetadata.UnknownLanguage, key.copy(variantId = variant).weeklyResultMetadata())
        }
        val ordinary = GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintClassic, null)
        assertEquals(WeeklyResultMetadata.None, ordinary.weeklyResultMetadata())
        assertEquals(
            WeeklyResultMetadata.UnknownVariant,
            ordinary.copy(variantId = "raw-secret-id").weeklyResultMetadata(),
        )
    }

    @Test
    fun signedIntegerDeltaKeepsPlusUnicodeMinusAndPlainZero() {
        assertEquals("+2", 2.signedWeeklyChange())
        assertEquals("−2", (-2).signedWeeklyChange())
        assertEquals("0", 0.signedWeeklyChange())
        assertEquals("+2147483647", Int.MAX_VALUE.signedWeeklyChange())
        assertEquals("−2147483647", (-Int.MAX_VALUE).signedWeeklyChange())
    }

    @Test
    fun allExistingStatsGameAndModeMappingsRemainAvailable() {
        GameId.entries.forEach { assertEquals(it.name, it.toStatsGame().name) }
        GameModeId.entries.forEach { mode ->
            val gamePrefix = GameId.entries.first { mode.name.startsWith(it.name) }.name
            assertEquals(mode.name.removePrefix(gamePrefix), mode.toStatsMode().name)
        }
    }

    private fun custom(variant: String?) =
        GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintCustom, variant).weeklyResultMetadata()
}
