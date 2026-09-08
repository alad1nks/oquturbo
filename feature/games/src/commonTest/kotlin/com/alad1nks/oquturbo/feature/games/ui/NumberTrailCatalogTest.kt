package com.alad1nks.oquturbo.feature.games.ui

import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberTrailCatalogTest {
    @Test
    fun numberTrailIsTheEighthDirectGameWithAttentionAndVision() {
        val games = GamesUiState().games
        assertEquals(8, games.size)
        val numberTrail = games[7]
        assertEquals(TrainingGame.NumberTrail, numberTrail.game)
        assertEquals(1, numberTrail.modesCount)
        assertEquals(listOf(GamesUiState.Skill.Attention, GamesUiState.Skill.Vision), numberTrail.skills)
        assertTrue(GamesUiState().upcomingGames.isEmpty())
    }
}
