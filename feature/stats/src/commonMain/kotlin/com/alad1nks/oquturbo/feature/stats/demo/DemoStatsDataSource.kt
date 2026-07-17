package com.alad1nks.oquturbo.feature.stats.demo

import com.alad1nks.oquturbo.feature.stats.data.StatsDataSource
import com.alad1nks.oquturbo.feature.stats.model.ActivityStatus
import com.alad1nks.oquturbo.feature.stats.model.GameStatsRow
import com.alad1nks.oquturbo.feature.stats.model.GameTrend
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.RecentActivity
import com.alad1nks.oquturbo.feature.stats.model.RecentActivityType
import com.alad1nks.oquturbo.feature.stats.model.SkillInsight
import com.alad1nks.oquturbo.feature.stats.model.StatsDayActivity
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot
import com.alad1nks.oquturbo.feature.stats.model.StatsSkill
import com.alad1nks.oquturbo.feature.stats.model.StatsSummary
import com.alad1nks.oquturbo.feature.stats.model.StatsTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsWeekday

/**
 * Deliberately isolated preview/demo data. It is not a source of product progress and can be replaced
 * by a persistent [StatsDataSource] without changing the screen contract.
 */
internal class DemoStatsDataSource : StatsDataSource {
    override fun getSnapshot(period: StatsPeriod): StatsPeriodSnapshot = DemoStatsFixtures.snapshot(period)
}

internal object DemoStatsFixtures {
    fun empty(): StatsUiState = StatsUiState(snapshot = StatsPeriodSnapshot.Empty)

    fun populated(): StatsUiState = stateFrom(snapshot(StatsPeriod.SevenDays, compact = true))

    fun rich(): StatsUiState = stateFrom(snapshot(StatsPeriod.ThirtyDays), StatsPeriod.ThirtyDays)

    fun oneMode(): StatsUiState {
        val source = snapshot(StatsPeriod.SevenDays, compact = true)
        val onlyTrend = source.trends.first().copy(modes = source.trends.first().modes.take(1))
        val gamesPlayed = onlyTrend.modes.sumOf { it.gamesPlayed }
        val onlyGame =
            source.games.first().copy(
                gamesPlayed = gamesPlayed,
                minutes = gamesPlayed * 2,
                totalModes = 1,
                modesWithRecords = onlyTrend.modes.count { it.scores.isNotEmpty() },
            )
        val days =
            activityDays(
                count = 7,
                totalGames = gamesPlayed,
                totalMinutes = onlyGame.minutes,
            )
        return stateFrom(
            source.copy(
                summary =
                    source.summary.copy(
                        trainings = days.count { it.status == ActivityStatus.DailyComplete },
                        games = days.sumOf { it.games },
                        minutes = days.sumOf { it.minutes },
                        correctAnswers = gamesPlayed * 7,
                    ),
                activityDays = days,
                trends = listOf(onlyTrend),
                games = listOf(onlyGame),
            ),
        )
    }

    fun multiMode(): StatsUiState = populated()

    fun noActivity(): StatsUiState {
        val days = activityDays(count = 7, totalGames = 0, totalMinutes = 0)
        return stateFrom(
            StatsPeriodSnapshot(
                isNewUser = false,
                summary = StatsSummary(0, 0, 0, 0),
                activityDays = days,
                trends = emptyList(),
                skills = emptyList(),
                games = emptyList(),
                recentActivity = emptyList(),
            ),
        )
    }

    fun newRecord(): StatsUiState {
        val source = snapshot(StatsPeriod.SevenDays)
        val firstGame = source.trends.first()
        val firstMode = firstGame.modes.first().copy(hasNewRecord = true)
        val updatedGame = firstGame.copy(modes = listOf(firstMode) + firstGame.modes.drop(1))
        return stateFrom(
            source.copy(
                trends = listOf(updatedGame) + source.trends.drop(1),
                recentActivity =
                    listOf(
                        RecentActivity(
                            type = RecentActivityType.NewRecord,
                            game = StatsGame.NumberSprint,
                            mode = StatsMode.Classic,
                            score = firstMode.record,
                        ),
                    ) + source.recentActivity,
            ),
        )
    }

    fun snapshot(
        period: StatsPeriod,
        compact: Boolean = false,
    ): StatsPeriodSnapshot {
        val scale =
            when (period) {
                StatsPeriod.SevenDays -> 1
                StatsPeriod.ThirtyDays -> 3
                StatsPeriod.AllTime -> 9
            }
        val dayCount =
            when (period) {
                StatsPeriod.SevenDays -> 7
                StatsPeriod.ThirtyDays -> 30
                StatsPeriod.AllTime -> 28
            }
        val trends = if (compact) compactGameTrends() else gameTrends(scale)
        val games = gameRows(trends = trends, scale = scale, compact = compact)
        val gamesPlayed = games.sumOf { it.gamesPlayed }
        val minutesPlayed = games.sumOf { it.minutes }
        val days =
            activityDays(
                count = dayCount,
                totalGames = gamesPlayed,
                totalMinutes = minutesPlayed,
            )

        return StatsPeriodSnapshot(
            isNewUser = false,
            summary =
                StatsSummary(
                    trainings = days.count { it.status == ActivityStatus.DailyComplete },
                    games = days.sumOf { it.games },
                    minutes = days.sumOf { it.minutes },
                    correctAnswers = if (compact) gamesPlayed * 7 else 326 * scale,
                ),
            activityDays = days,
            trends = trends,
            skills = skillInsights(scale),
            games = games,
            recentActivity = recentActivities(),
        )
    }

    private fun stateFrom(
        snapshot: StatsPeriodSnapshot,
        period: StatsPeriod = StatsPeriod.SevenDays,
    ): StatsUiState {
        val firstGame = snapshot.trends.firstOrNull()
        return StatsUiState(
            period = period,
            snapshot = snapshot,
            selectedDayId = snapshot.activityDays.lastOrNull()?.id,
            selectedGame = firstGame?.game,
            selectedMode = firstGame?.modes?.firstOrNull()?.mode,
        )
    }

    private fun activityDays(
        count: Int,
        totalGames: Int,
        totalMinutes: Int,
    ): List<StatsDayActivity> {
        val initialStatuses =
            List(count) { index ->
                when (index % 7) {
                    0, 3, 6 -> ActivityStatus.DailyComplete
                    1 -> ActivityStatus.DailyPartial
                    2, 5 -> ActivityStatus.GamesOnly
                    else -> ActivityStatus.None
                }
            }
        val activeIndexes = initialStatuses.indices.filter { initialStatuses[it] != ActivityStatus.None }
        val retainedActivityCount = minOf(activeIndexes.size, totalGames, totalMinutes)
        val retainedIndexes = activeIndexes.take(retainedActivityCount).toSet()

        return List(count) { index ->
            val status =
                if (index in retainedIndexes) {
                    initialStatuses[index]
                } else {
                    ActivityStatus.None
                }
            val activityIndex = activeIndexes.indexOf(index)
            StatsDayActivity(
                id = index,
                dayNumber = index + 1,
                weekday = StatsWeekday.entries[index % StatsWeekday.entries.size],
                status = status,
                games = distributedValue(totalGames, activityIndex, retainedActivityCount),
                minutes = distributedValue(totalMinutes, activityIndex, retainedActivityCount),
            )
        }
    }

    private fun distributedValue(
        total: Int,
        index: Int,
        itemCount: Int,
    ): Int {
        if (index !in 0 until itemCount || itemCount == 0) return 0
        return total / itemCount + if (index < total % itemCount) 1 else 0
    }

    private fun gameTrends(scale: Int): List<GameTrend> =
        listOf(
            GameTrend(
                game = StatsGame.NumberSprint,
                modes =
                    listOf(
                        mode(StatsMode.Classic, listOf(18, 22, 21, 28, 25, 31, 38, 54), scale, 8),
                        mode(StatsMode.Binary, listOf(12, 14, 13, 17, 19, 22), scale, 4),
                        mode(StatsMode.Custom, listOf(9, 11, 10, 15, 16), scale, null),
                    ),
            ),
            GameTrend(
                game = StatsGame.WideEye,
                modes =
                    listOf(
                        mode(StatsMode.Characters, listOf(16, 18, 21, 20, 24, 31), scale, 5),
                        mode(StatsMode.Words, listOf(11, 13, 15, 14, 18), scale, 3),
                        mode(StatsMode.FindDifference, listOf(8, 10, 9, 12), scale, null),
                        mode(StatsMode.WideLine, listOf(14, 15, 18, 20), scale, 4),
                    ),
            ),
            GameTrend(
                game = StatsGame.DontTap,
                modes =
                    listOf(
                        mode(StatsMode.Categories, listOf(20, 22, 26, 25, 28), scale, 6),
                        mode(StatsMode.Letter, listOf(17, 19, 21, 23), scale, null),
                        mode(StatsMode.WordLength, listOf(13, 16, 15, 19), scale, 2),
                        mode(StatsMode.TextColor, listOf(10, 12, 14, 13, 17), scale, 5),
                        mode(StatsMode.TrueFalse, listOf(18, 20, 21, 24), scale, null),
                        mode(StatsMode.Math, listOf(9, 11, 12, 15), scale, 3),
                        mode(StatsMode.SpeedReading, listOf(15, 17, 19, 22), scale, 4),
                    ),
            ),
        )

    private fun compactGameTrends(): List<GameTrend> =
        listOf(
            GameTrend(
                game = StatsGame.NumberSprint,
                modes =
                    listOf(
                        mode(StatsMode.Classic, listOf(38, 54), scale = 1, comparison = null),
                        mode(StatsMode.Binary, listOf(19, 22), scale = 1, comparison = null),
                    ),
            ),
            GameTrend(
                game = StatsGame.WideEye,
                modes =
                    listOf(
                        mode(StatsMode.Characters, listOf(31), scale = 1, comparison = null),
                    ),
            ),
        )

    private fun mode(
        mode: StatsMode,
        scores: List<Int>,
        scale: Int,
        comparison: Int?,
    ): ModeTrend {
        val repeatedScores = List(scale.coerceAtMost(4)) { scores }.flatten()
        return ModeTrend(
            mode = mode,
            scores = repeatedScores,
            record = scores.max(),
            lastResult = scores.last(),
            averageResult = scores.average().toInt(),
            gamesPlayed = repeatedScores.size,
            comparisonPercent = comparison,
        )
    }

    private fun skillInsights(scale: Int): List<SkillInsight> =
        listOf(
            SkillInsight(StatsSkill.Memory, 8 * scale, 6, StatsTrend.Growing),
            SkillInsight(StatsSkill.Attention, 10 * scale, 4, StatsTrend.Growing),
            SkillInsight(StatsSkill.Reaction, 9 * scale, 2, StatsTrend.Stable),
            SkillInsight(StatsSkill.PeripheralVision, 5 * scale, null, StatsTrend.NotEnoughData),
            SkillInsight(StatsSkill.RecognitionSpeed, 6 * scale, -2, StatsTrend.Declining),
        )

    private fun gameRows(
        trends: List<GameTrend>,
        scale: Int,
        compact: Boolean,
    ): List<GameStatsRow> =
        trends.map { trend ->
            val gamesPlayed = trend.modes.sumOf { it.gamesPlayed }
            GameStatsRow(
                game = trend.game,
                gamesPlayed = gamesPlayed,
                minutes =
                    if (compact) {
                        gamesPlayed * 2
                    } else {
                        when (trend.game) {
                            StatsGame.NumberSprint -> 38 * scale
                            StatsGame.WideEye -> 29 * scale
                            StatsGame.DontTap -> 17 * scale
                        }
                    },
                modesWithRecords = trend.modes.count { it.scores.isNotEmpty() },
                totalModes = trend.modes.size,
            )
        }

    private fun recentActivities(): List<RecentActivity> =
        listOf(
            RecentActivity(
                type = RecentActivityType.NewRecord,
                game = StatsGame.NumberSprint,
                mode = StatsMode.Classic,
                score = 54,
            ),
            RecentActivity(type = RecentActivityType.DailyTraining, daysAgo = 1),
            RecentActivity(
                type = RecentActivityType.GameResult,
                game = StatsGame.WideEye,
                mode = StatsMode.Characters,
                score = 31,
                daysAgo = 2,
            ),
        )
}
