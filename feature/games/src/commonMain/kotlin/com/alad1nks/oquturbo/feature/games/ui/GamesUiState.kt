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
                modesCount = 4,
            ),
            GameSummary(
                game = TrainingGame.DontTap,
                skills = listOf(Skill.Attention, Skill.Reaction),
                modesCount = 7,
            ),
            GameSummary(
                game = TrainingGame.MemoryGrid,
                skills = listOf(Skill.Memory, Skill.Attention),
                modesCount = 3,
            ),
            GameSummary(
                game = TrainingGame.WordFlow,
                skills = listOf(Skill.Reading),
                modesCount = 1,
            ),
            GameSummary(
                game = TrainingGame.DualFocus,
                skills = listOf(Skill.Attention),
                modesCount = 1,
            ),
        ),
    val upcomingGames: List<UpcomingGame> = emptyList(),
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
        DualFocus,
    }
}
