package com.alad1nks.oquturbo.feature.games.ui

import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SymbolCountCatalogTest {
    @Test
    fun symbolCountIsTheNinthDirectGameWithAttentionAndVision() {
        val games = GamesUiState().games
        assertEquals(10, games.size)
        val symbolCount = games[8]
        assertEquals(TrainingGame.SymbolCount, symbolCount.game)
        assertEquals(1, symbolCount.modesCount)
        assertEquals(listOf(GamesUiState.Skill.Attention, GamesUiState.Skill.Vision), symbolCount.skills)
        assertTrue(GamesUiState().upcomingGames.isEmpty())
    }
}
