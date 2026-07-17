package com.alad1nks.oquturbo.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameId {
    NumberSprint,
    WideEye,
    DontTap,
}

@Serializable
enum class GameModeId {
    NumberSprintClassic,
    NumberSprintBinary,
    NumberSprintCustom,
    WideEyeCharacters,
    WideEyeWords,
    WideEyeFindDifference,
    WideEyeWideLine,
    DontTapCategories,
    DontTapLetter,
    DontTapWordLength,
    DontTapTextColor,
    DontTapTrueFalse,
    DontTapMath,
    DontTapSpeedReading,
}

@Serializable
data class GameSession(
    val game: GameId,
    val mode: GameModeId,
    val variantId: String? = null,
    val score: Int,
    val correctAnswers: Int,
    val durationMillis: Long,
    val completedAtEpochMillis: Long,
    val completedEpochDay: Long,
    val isNewRecord: Boolean,
)

@Serializable
data class GameRecord(
    val game: GameId,
    val mode: GameModeId,
    val variantId: String? = null,
    val score: Int,
)

@Serializable
data class GameActivityTotals(
    val sessionCount: Long = 0,
    val durationMillis: Long = 0,
    val correctAnswers: Long = 0,
    val series: List<GameSeriesTotals> = emptyList(),
)

@Serializable
data class GameSeriesTotals(
    val game: GameId,
    val mode: GameModeId,
    val variantId: String? = null,
    val sessionCount: Long = 0,
    val durationMillis: Long = 0,
    val correctAnswers: Long = 0,
    val scoreTotal: Long = 0,
)

@Serializable
data class PlayerProgress(
    val totalCorrectAnswers: Int,
    val totalXp: Int,
    val level: Int,
    val currentLevelXp: Int,
    val xpPerLevel: Int = 500,
)
