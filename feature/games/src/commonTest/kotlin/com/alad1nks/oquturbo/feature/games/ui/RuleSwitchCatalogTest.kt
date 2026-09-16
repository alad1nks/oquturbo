package com.alad1nks.oquturbo.feature.games.ui

import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleSwitchCatalogTest {
    @Test
    fun ruleSwitchIsTheTenthDirectGameWithAttention() {
        val games = GamesUiState().games
        assertEquals(10, games.size)
        val ruleSwitch = games[9]
        assertEquals(TrainingGame.RuleSwitch, ruleSwitch.game)
        assertEquals(1, ruleSwitch.modesCount)
        assertEquals(listOf(GamesUiState.Skill.Attention), ruleSwitch.skills)
        assertTrue(GamesUiState().upcomingGames.isEmpty())
    }
}
