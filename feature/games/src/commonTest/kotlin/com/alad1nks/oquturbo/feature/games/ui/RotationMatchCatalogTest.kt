package com.alad1nks.oquturbo.feature.games.ui

import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotationMatchCatalogTest {
    @Test
    fun rotationMatchIsTheSeventhDirectGameWithOneVisionMode() {
        val games = GamesUiState().games
        assertEquals(7, games.size)
        val rotationMatch = games[6]
        assertEquals(TrainingGame.RotationMatch, rotationMatch.game)
        assertEquals(1, rotationMatch.modesCount)
        assertEquals(listOf(GamesUiState.Skill.Vision), rotationMatch.skills)
        assertTrue(GamesUiState().upcomingGames.isEmpty())
    }
}
