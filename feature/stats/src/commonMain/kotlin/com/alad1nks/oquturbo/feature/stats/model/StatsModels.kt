package com.alad1nks.oquturbo.feature.stats.model

import kotlinx.serialization.Serializable

@Serializable
enum class StatsGame {
    NumberSprint,
    WideEye,
    DontTap,
}

@Serializable
enum class StatsMode {
    Classic,
    Binary,
    Custom,
    Characters,
    Words,
    FindDifference,
    WideLine,
    Categories,
    Letter,
    WordLength,
    TextColor,
    TrueFalse,
    Math,
    SpeedReading,
}

@Serializable
enum class StatsPeriod {
    SevenDays,
    ThirtyDays,
    AllTime,
}

internal enum class ActivityStatus {
    DailyComplete,
    DailyPartial,
    GamesOnly,
    None,
}

internal enum class StatsWeekday {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday,
}

internal enum class StatsSkill {
    Memory,
    Attention,
    Reaction,
    PeripheralVision,
    RecognitionSpeed,
}

internal enum class StatsTrend {
    Growing,
    Declining,
    Stable,
    NotEnoughData,
}

internal enum class RecentActivityType {
    NewRecord,
    DailyTraining,
    GameResult,
    PersonalBest,
}

internal data class StatsSummary(
    val trainings: Int,
    val games: Int,
    val minutes: Int,
    val correctAnswers: Int,
)

internal data class StatsDayActivity(
    val id: Int,
    val dayNumber: Int,
    val weekday: StatsWeekday,
    val status: ActivityStatus,
    val games: Int,
    val minutes: Int,
)

internal data class ModeTrend(
    val mode: StatsMode,
    val scores: List<Int>,
    val record: Int,
    val lastResult: Int,
    val averageResult: Int,
    val gamesPlayed: Int,
    val comparisonPercent: Int? = null,
    val hasNewRecord: Boolean = false,
)

internal data class GameTrend(
    val game: StatsGame,
    val modes: List<ModeTrend>,
)

internal data class SkillInsight(
    val skill: StatsSkill,
    val trainings: Int,
    val averageChangePercent: Int?,
    val trend: StatsTrend,
)

internal data class GameStatsRow(
    val game: StatsGame,
    val gamesPlayed: Int,
    val minutes: Int,
    val modesWithRecords: Int,
    val totalModes: Int,
)

internal data class RecentActivity(
    val type: RecentActivityType,
    val game: StatsGame? = null,
    val mode: StatsMode? = null,
    val score: Int? = null,
    val daysAgo: Int = 0,
)

internal data class StatsPeriodSnapshot(
    val isNewUser: Boolean,
    val summary: StatsSummary,
    val activityDays: List<StatsDayActivity>,
    val trends: List<GameTrend>,
    val skills: List<SkillInsight>,
    val games: List<GameStatsRow>,
    val recentActivity: List<RecentActivity>,
) {
    val hasActivity: Boolean
        get() = summary.games > 0 || summary.trainings > 0

    companion object {
        val Empty =
            StatsPeriodSnapshot(
                isNewUser = true,
                summary = StatsSummary(0, 0, 0, 0),
                activityDays = emptyList(),
                trends = emptyList(),
                skills = emptyList(),
                games = emptyList(),
                recentActivity = emptyList(),
            )
    }
}

internal data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.SevenDays,
    val snapshot: StatsPeriodSnapshot = StatsPeriodSnapshot.Empty,
    val selectedDayId: Int? = null,
    val selectedGame: StatsGame? = null,
    val selectedMode: StatsMode? = null,
) {
    val selectedDay: StatsDayActivity?
        get() = snapshot.activityDays.firstOrNull { it.id == selectedDayId }

    val selectedGameTrend: GameTrend?
        get() = snapshot.trends.firstOrNull { it.game == selectedGame }

    val selectedModeTrend: ModeTrend?
        get() = selectedGameTrend?.modes?.firstOrNull { it.mode == selectedMode }
}

internal data class StatsDetailUiState(
    val game: StatsGame,
    val period: StatsPeriod,
    val modes: List<ModeTrend>,
    val selectedMode: StatsMode? = null,
)
