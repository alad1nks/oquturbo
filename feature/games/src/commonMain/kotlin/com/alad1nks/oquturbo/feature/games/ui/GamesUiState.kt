package com.alad1nks.oquturbo.feature.games.ui

import com.alad1nks.oquturbo.feature.games.model.TrainingGame

internal data class GamesUiState(
    val games: List<GameSummary> =
        listOf(
            GameSummary(
                game = TrainingGame.NumberSprint,
                skills = listOf(Skill.Memory, Skill.Reaction),
                modesCount = 3,
            ),
            GameSummary(
                game = TrainingGame.WideEye,
                skills = listOf(Skill.Attention, Skill.Vision),
                modesCount = 2,
            ),
            GameSummary(
                game = TrainingGame.DontTap,
                skills = listOf(Skill.Attention, Skill.Reaction),
                modesCount = 2,
            ),
        ),
    val upcomingGames: List<UpcomingGame> = UpcomingGame.entries,
) {
    data class GameSummary(
        val game: TrainingGame,
        val skills: List<Skill>,
        val modesCount: Int,
    )

    enum class Skill {
        Memory,
        Attention,
        Reaction,
        Reading,
        Vision,
    }

    enum class UpcomingGame {
        MemoryGrid,
        DualFocus,
        WordFlow,
    }
}
