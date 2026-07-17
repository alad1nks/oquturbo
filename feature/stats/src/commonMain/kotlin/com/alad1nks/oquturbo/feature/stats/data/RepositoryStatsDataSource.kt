package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.core.data.model.GameActivityTotals
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameRecord
import com.alad1nks.oquturbo.core.data.model.GameSeriesTotals
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
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
import com.alad1nks.oquturbo.feature.stats.model.StatsWeekday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToInt
import kotlin.time.Clock

internal class RepositoryStatsDataSource(
    private val repository: GameActivityRepository,
    private val currentEpochDay: () -> Long = ::utcEpochDay,
) : StatsDataSource {
    override fun observeSnapshot(period: StatsPeriod): Flow<StatsPeriodSnapshot> =
        combine(
            repository.observeSessions(),
            repository.observeRecords(),
            repository.observeTotals(),
        ) { sessions, records, totals ->
            createSnapshot(
                period = period,
                sessions = sessions,
                records = records,
                totals = totals,
                todayEpochDay = currentEpochDay(),
            )
        }
}

private fun createSnapshot(
    period: StatsPeriod,
    sessions: List<GameSession>,
    records: List<GameRecord>,
    totals: GameActivityTotals,
    todayEpochDay: Long,
): StatsPeriodSnapshot {
    val periodWindow = period.window(todayEpochDay)
    val currentSessions =
        sessions
            .filter { session -> periodWindow.current?.contains(session.completedEpochDay) != false }
            .sortedBy(GameSession::completedAtEpochMillis)
    val previousSessions =
        periodWindow.previous?.let { previous ->
            sessions.filter { session -> session.completedEpochDay in previous }
        }.orEmpty()

    return StatsPeriodSnapshot(
        isNewUser = totals.sessionCount == 0L && records.isEmpty(),
        summary = if (period == StatsPeriod.AllTime) totals.toSummary() else currentSessions.toSummary(),
        activityDays =
            createActivityDays(
                sessions = sessions,
                range = periodWindow.activity,
            ),
        trends =
            createGameTrends(
                period = period,
                currentSessions = currentSessions,
                previousSessions = previousSessions,
                allSessions = sessions,
                records = records,
                totals = totals,
            ),
        skills =
            createSkillInsights(
                period = period,
                currentSessions = currentSessions,
                previousSessions = previousSessions,
                totals = totals,
            ),
        games =
            createGameRows(
                period = period,
                currentSessions = currentSessions,
                records = records,
                totals = totals,
            ),
        recentActivity = createRecentActivity(currentSessions, todayEpochDay),
    )
}

private fun StatsPeriod.window(today: Long): PeriodWindow =
    when (this) {
        StatsPeriod.SevenDays -> finiteWindow(today = today, dayCount = 7)
        StatsPeriod.ThirtyDays -> finiteWindow(today = today, dayCount = 30)
        StatsPeriod.AllTime ->
            PeriodWindow(
                current = null,
                previous = null,
                activity = (today - ALL_TIME_ACTIVITY_DAYS + 1)..today,
            )
    }

private fun finiteWindow(
    today: Long,
    dayCount: Long,
): PeriodWindow {
    val current = (today - dayCount + 1)..today
    return PeriodWindow(
        current = current,
        previous = (current.first - dayCount)..(current.first - 1),
        activity = current,
    )
}

private fun List<GameSession>.toSummary(): StatsSummary =
    StatsSummary(
        trainings = 0,
        games = size,
        minutes = sumOf(GameSession::durationMillis).toMinutesCount(),
        correctAnswers = sumOf { it.correctAnswers.toLong() }.toCount(),
    )

private fun GameActivityTotals.toSummary(): StatsSummary =
    StatsSummary(
        trainings = 0,
        games = sessionCount.toCount(),
        minutes = durationMillis.toMinutesCount(),
        correctAnswers = correctAnswers.toCount(),
    )

private fun createActivityDays(
    sessions: List<GameSession>,
    range: LongRange,
): List<StatsDayActivity> {
    val sessionsByDay = sessions.groupBy(GameSession::completedEpochDay)
    return range.map { epochDay ->
        val daySessions = sessionsByDay[epochDay].orEmpty()
        StatsDayActivity(
            id = epochDay.toCount(),
            dayNumber = epochDay.dayOfMonth(),
            weekday = epochDay.weekday(),
            status = if (daySessions.isEmpty()) ActivityStatus.None else ActivityStatus.GamesOnly,
            games = daySessions.size,
            minutes = daySessions.sumOf(GameSession::durationMillis).toMinutesCount(),
        )
    }
}

private fun createGameTrends(
    period: StatsPeriod,
    currentSessions: List<GameSession>,
    previousSessions: List<GameSession>,
    allSessions: List<GameSession>,
    records: List<GameRecord>,
    totals: GameActivityTotals,
): List<GameTrend> =
    GameId.entries.mapNotNull { game ->
        val modes =
            game.modeCatalog().flatMap { mode ->
                seriesVariants(
                    game = game,
                    mode = mode,
                    currentSessions = currentSessions,
                    allSessions = allSessions,
                    records = records,
                    totals = totals,
                ).mapNotNull { variantId ->
                    createModeTrend(
                        period = period,
                        game = game,
                        mode = mode,
                        variantId = variantId,
                        currentSessions = currentSessions,
                        previousSessions = previousSessions,
                        allSessions = allSessions,
                        records = records,
                        totals = totals,
                    )
                }
            }
        modes.takeIf(List<*>::isNotEmpty)?.let {
            GameTrend(
                game = game.toStatsGame(),
                modes = modes,
            )
        }
    }

private fun createModeTrend(
    period: StatsPeriod,
    game: GameId,
    mode: GameModeId,
    variantId: String?,
    currentSessions: List<GameSession>,
    previousSessions: List<GameSession>,
    allSessions: List<GameSession>,
    records: List<GameRecord>,
    totals: GameActivityTotals,
): ModeTrend? {
    val selectedSessions = currentSessions.inSeries(game, mode, variantId)
    val allSeriesSessions = allSessions.inSeries(game, mode, variantId)
    val storedRecord =
        records
            .asSequence()
            .filter { record -> record.belongsTo(game, mode, variantId) }
            .maxOfOrNull(GameRecord::score)
    val record = maxOf(storedRecord ?: 0, allSeriesSessions.maxOfOrNull(GameSession::score) ?: 0)
    val seriesTotals = totals.series.firstOrNull { it.belongsTo(game, mode, variantId) }
    val hasStoredRecord = storedRecord != null || allSeriesSessions.isNotEmpty() || seriesTotals != null

    if (selectedSessions.isEmpty() && (period != StatsPeriod.AllTime || !hasStoredRecord)) return null

    val scores = selectedSessions.map(GameSession::score)
    return ModeTrend(
        mode = mode.toStatsMode(),
        variantId = variantId,
        scores = scores,
        record = record,
        lastResult = scores.lastOrNull(),
        averageResult =
            if (period == StatsPeriod.AllTime) {
                seriesTotals?.averageScore()
            } else {
                scores.averageOrNull()?.roundToInt()
            },
        gamesPlayed =
            if (period == StatsPeriod.AllTime) {
                seriesTotals?.sessionCount?.toCount() ?: selectedSessions.size
            } else {
                selectedSessions.size
            },
        comparisonPercent =
            if (period == StatsPeriod.AllTime) {
                null
            } else {
                compareSeries(
                    current = selectedSessions,
                    previous = previousSessions.inSeries(game, mode, variantId),
                )
            },
        hasNewRecord = selectedSessions.any(GameSession::isNewRecord),
    )
}

private fun createSkillInsights(
    period: StatsPeriod,
    currentSessions: List<GameSession>,
    previousSessions: List<GameSession>,
    totals: GameActivityTotals,
): List<SkillInsight> =
    StatsSkill.entries.map { skill ->
        val skillGames = skill.gameIds()
        val current = currentSessions.filter { it.game in skillGames }
        val previous = previousSessions.filter { it.game in skillGames }
        val comparison =
            if (period == StatsPeriod.AllTime) {
                null
            } else {
                comparableSeriesChange(current = current, previous = previous)
            }
        SkillInsight(
            skill = skill,
            trainings =
                if (period == StatsPeriod.AllTime) {
                    totals.series.filter { it.game in skillGames }.sumOf { it.sessionCount }.toCount()
                } else {
                    current.size
                },
            averageChangePercent = comparison,
            showComparison = period != StatsPeriod.AllTime,
            trend =
                when {
                    comparison == null -> StatsTrend.NotEnoughData
                    comparison > 0 -> StatsTrend.Growing
                    comparison < 0 -> StatsTrend.Declining
                    else -> StatsTrend.Stable
                },
        )
    }

private fun comparableSeriesChange(
    current: List<GameSession>,
    previous: List<GameSession>,
): Int? {
    val currentGroups = current.groupBy(GameSession::seriesKey)
    val previousGroups = previous.groupBy(GameSession::seriesKey)
    val changes =
        currentGroups.mapNotNull { (key, currentSeries) ->
            val previousSeries = previousGroups[key].orEmpty()
            compareSeries(currentSeries, previousSeries)?.let { change ->
                WeightedChange(percent = change, weight = currentSeries.size)
            }
        }
    val totalWeight = changes.sumOf(WeightedChange::weight)
    if (totalWeight == 0) return null
    return changes.sumOf { it.percent * it.weight }.toDouble().div(totalWeight).roundToInt()
}

private fun compareSeries(
    current: List<GameSession>,
    previous: List<GameSession>,
): Int? {
    if (current.size < MINIMUM_COMPARISON_GAMES || previous.size < MINIMUM_COMPARISON_GAMES) return null
    val currentAverage = current.map(GameSession::score).average()
    val previousAverage = previous.map(GameSession::score).average()
    if (previousAverage <= 0.0) return null
    return ((currentAverage - previousAverage) / previousAverage * 100).roundToInt()
}

private fun createGameRows(
    period: StatsPeriod,
    currentSessions: List<GameSession>,
    records: List<GameRecord>,
    totals: GameActivityTotals,
): List<GameStatsRow> =
    GameId.entries.mapNotNull { game ->
        val gameSessions = currentSessions.filter { it.game == game }
        val gameTotals = totals.series.filter { it.game == game }
        val recordedModes = records.filter { it.game == game }.mapTo(mutableSetOf()) { it.mode }
        val showLegacyOnly = period == StatsPeriod.AllTime && (recordedModes.isNotEmpty() || gameTotals.isNotEmpty())
        if (gameSessions.isEmpty() && !showLegacyOnly) return@mapNotNull null

        GameStatsRow(
            game = game.toStatsGame(),
            gamesPlayed =
                if (period == StatsPeriod.AllTime) {
                    gameTotals.sumOf { it.sessionCount }.toCount()
                } else {
                    gameSessions.size
                },
            minutes =
                if (period == StatsPeriod.AllTime) {
                    gameTotals.sumOf { it.durationMillis }.toMinutesCount()
                } else {
                    gameSessions.sumOf(GameSession::durationMillis).toMinutesCount()
                },
            modesWithRecords = recordedModes.size,
            totalModes = game.modeCatalog().size,
        )
    }

private fun createRecentActivity(
    sessions: List<GameSession>,
    todayEpochDay: Long,
): List<RecentActivity> =
    sessions
        .asSequence()
        .sortedByDescending(GameSession::completedAtEpochMillis)
        .take(RECENT_ACTIVITY_LIMIT)
        .map { session ->
            RecentActivity(
                type =
                    if (session.isNewRecord) {
                        RecentActivityType.NewRecord
                    } else {
                        RecentActivityType.GameResult
                    },
                game = session.game.toStatsGame(),
                mode = session.mode.toStatsMode(),
                variantId = session.variantId,
                score = session.score,
                daysAgo = (todayEpochDay - session.completedEpochDay).coerceAtLeast(0).toCount(),
            )
        }.toList()

private fun seriesVariants(
    game: GameId,
    mode: GameModeId,
    currentSessions: List<GameSession>,
    allSessions: List<GameSession>,
    records: List<GameRecord>,
    totals: GameActivityTotals,
): List<String?> {
    if (mode != GameModeId.NumberSprintCustom) return listOf(null)
    return buildList {
        currentSessions
            .asReversed()
            .filter { it.game == game && it.mode == mode }
            .mapNotNullTo(this) { it.variantId }
        allSessions
            .asReversed()
            .filter { it.game == game && it.mode == mode }
            .mapNotNullTo(this) { it.variantId }
        records
            .filter { it.game == game && it.mode == mode }
            .mapNotNullTo(this) { it.variantId }
        totals.series
            .filter { it.game == game && it.mode == mode }
            .mapNotNullTo(this) { it.variantId }
    }.distinct()
}

private fun List<GameSession>.inSeries(
    game: GameId,
    mode: GameModeId,
    variantId: String?,
): List<GameSession> = filter { it.belongsTo(game, mode, variantId) }

private fun GameSession.belongsTo(
    game: GameId,
    mode: GameModeId,
    variantId: String?,
): Boolean =
    this.game == game &&
        this.mode == mode &&
        (mode != GameModeId.NumberSprintCustom || this.variantId == variantId)

private fun GameRecord.belongsTo(
    game: GameId,
    mode: GameModeId,
    variantId: String?,
): Boolean =
    this.game == game &&
        this.mode == mode &&
        (mode != GameModeId.NumberSprintCustom || this.variantId == variantId)

private fun GameSeriesTotals.belongsTo(
    game: GameId,
    mode: GameModeId,
    variantId: String?,
): Boolean =
    this.game == game &&
        this.mode == mode &&
        (mode != GameModeId.NumberSprintCustom || this.variantId == variantId)

private fun GameSeriesTotals.averageScore(): Int? =
    sessionCount.takeIf { it > 0 }?.let { (scoreTotal.toDouble() / it).roundToInt() }

private fun GameSession.seriesKey() =
    SeriesKey(
        game = game,
        mode = mode,
        variantId = variantId.takeIf { mode == GameModeId.NumberSprintCustom },
    )

private fun GameId.modeCatalog(): List<GameModeId> =
    when (this) {
        GameId.NumberSprint ->
            listOf(
                GameModeId.NumberSprintClassic,
                GameModeId.NumberSprintBinary,
                GameModeId.NumberSprintCustom,
            )
        GameId.WideEye ->
            listOf(
                GameModeId.WideEyeCharacters,
                GameModeId.WideEyeWords,
                GameModeId.WideEyeFindDifference,
                GameModeId.WideEyeWideLine,
            )
        GameId.DontTap ->
            listOf(
                GameModeId.DontTapCategories,
                GameModeId.DontTapLetter,
                GameModeId.DontTapWordLength,
                GameModeId.DontTapTextColor,
                GameModeId.DontTapTrueFalse,
                GameModeId.DontTapMath,
                GameModeId.DontTapSpeedReading,
            )
    }

private fun GameId.toStatsGame(): StatsGame =
    when (this) {
        GameId.NumberSprint -> StatsGame.NumberSprint
        GameId.WideEye -> StatsGame.WideEye
        GameId.DontTap -> StatsGame.DontTap
    }

private fun GameModeId.toStatsMode(): StatsMode =
    when (this) {
        GameModeId.NumberSprintClassic -> StatsMode.Classic
        GameModeId.NumberSprintBinary -> StatsMode.Binary
        GameModeId.NumberSprintCustom -> StatsMode.Custom
        GameModeId.WideEyeCharacters -> StatsMode.Characters
        GameModeId.WideEyeWords -> StatsMode.Words
        GameModeId.WideEyeFindDifference -> StatsMode.FindDifference
        GameModeId.WideEyeWideLine -> StatsMode.WideLine
        GameModeId.DontTapCategories -> StatsMode.Categories
        GameModeId.DontTapLetter -> StatsMode.Letter
        GameModeId.DontTapWordLength -> StatsMode.WordLength
        GameModeId.DontTapTextColor -> StatsMode.TextColor
        GameModeId.DontTapTrueFalse -> StatsMode.TrueFalse
        GameModeId.DontTapMath -> StatsMode.Math
        GameModeId.DontTapSpeedReading -> StatsMode.SpeedReading
    }

private fun StatsSkill.gameIds(): Set<GameId> =
    when (this) {
        StatsSkill.Memory -> setOf(GameId.NumberSprint)
        StatsSkill.Attention -> setOf(GameId.WideEye, GameId.DontTap)
        StatsSkill.Reaction -> setOf(GameId.NumberSprint, GameId.DontTap)
        StatsSkill.PeripheralVision,
        StatsSkill.RecognitionSpeed,
        -> setOf(GameId.WideEye)
    }

private fun Long.toMinutesCount(): Int =
    coerceAtLeast(0).div(MILLIS_PER_MINUTE).toCount()

private fun Long.toCount(): Int = coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

private fun List<Int>.averageOrNull(): Double? = takeIf(List<*>::isNotEmpty)?.average()

private fun Long.weekday(): StatsWeekday {
    val index = (((this + EPOCH_THURSDAY_OFFSET) % DAYS_PER_WEEK + DAYS_PER_WEEK) % DAYS_PER_WEEK).toInt()
    return StatsWeekday.entries[index]
}

private fun Long.dayOfMonth(): Int {
    val shiftedDays = this + DAYS_FROM_CIVIL_TO_UNIX_EPOCH
    val era = if (shiftedDays >= 0) shiftedDays / DAYS_PER_ERA else (shiftedDays - DAYS_PER_ERA + 1) / DAYS_PER_ERA
    val dayOfEra = shiftedDays - era * DAYS_PER_ERA
    val yearOfEra =
        (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    return (dayOfYear - (153 * monthPrime + 2) / 5 + 1).toInt()
}

private fun utcEpochDay(): Long = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY

private data class PeriodWindow(
    val current: LongRange?,
    val previous: LongRange?,
    val activity: LongRange,
)

private data class SeriesKey(
    val game: GameId,
    val mode: GameModeId,
    val variantId: String?,
)

private data class WeightedChange(
    val percent: Int,
    val weight: Int,
)

private const val MINIMUM_COMPARISON_GAMES = 3
private const val RECENT_ACTIVITY_LIMIT = 5
private const val ALL_TIME_ACTIVITY_DAYS = 28L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_DAY = 86_400_000L
private const val DAYS_PER_WEEK = 7L
private const val EPOCH_THURSDAY_OFFSET = 3L
private const val DAYS_FROM_CIVIL_TO_UNIX_EPOCH = 719_468L
private const val DAYS_PER_ERA = 146_097L
