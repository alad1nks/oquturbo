package com.alad1nks.oquturbo.shared.navigation

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressStatsMappingTest {
    @Test
    fun allSavedGamesAndModesOpenCorrespondingStatsEnums() {
        GameId.entries.forEach { assertEquals(it.name, it.toProgressStatsGame().name) }
        GameModeId.entries.forEach { mode ->
            val gamePrefix = GameId.entries.first { mode.name.startsWith(it.name) }.name
            assertEquals(mode.name.removePrefix(gamePrefix), mode.toProgressStatsMode().name)
        }
    }
}
