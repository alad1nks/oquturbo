package com.alad1nks.oquturbo.core.data.progress

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class CalculateProgressComparisonTest {
    @Test
    fun emptyOldAndFutureInputsHaveNoRecentSessions() {
        val expected = ProgressComparison.NoRecentSessions(TODAY - 27, TODAY)
        assertEquals(expected, calculateProgressComparison(emptyList(), TODAY))
        assertEquals(expected, calculateProgressComparison(listOf(session(day = TODAY - 28)), TODAY))
        assertEquals(expected, calculateProgressComparison(listOf(session(day = TODAY + 1)), TODAY))
    }

    @Test
    fun inclusiveUtcDayWindowUsesPersistedDayAndRecalculatesAtRollover() {
        val oldest = session(day = TODAY - 27, timestamp = 1)
        val newest = session(day = TODAY, timestamp = 2)
        val future = session(day = TODAY + 1, timestamp = 3, variant = "tomorrow")
        val sessions = listOf(oldest, newest, future, session(day = TODAY - 28, timestamp = 4))
        val today = assertIs<ProgressComparison.InsufficientData>(calculateProgressComparison(sessions, TODAY))
        assertEquals(2, today.availableCount)
        assertEquals(TODAY - 27, today.firstEpochDay)
        assertEquals(TODAY, today.lastEpochDay)
        val tomorrow = assertIs<ProgressComparison.InsufficientData>(calculateProgressComparison(sessions, TODAY + 1))
        assertEquals(GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintClassic, "tomorrow"), tomorrow.series)
        assertEquals(1, tomorrow.availableCount)
        assertEquals(TODAY - 26, tomorrow.firstEpochDay)
        assertEquals(TODAY + 1, tomorrow.lastEpochDay)
        // No time-of-day input: persisted today is included even with a later timestamp.
        assertEquals(
            1,
            assertIs<ProgressComparison.InsufficientData>(
                calculateProgressComparison(listOf(session(timestamp = Long.MAX_VALUE)), TODAY),
            ).availableCount,
        )
    }

    @Test
    fun latestTimestampWinsUnsortedInputAndInputOrderBreaksTimestampTies() {
        val first = session(timestamp = 50, variant = "first")
        val second = session(timestamp = 50, variant = "second")
        val old = session(timestamp = 49, variant = "old")
        val result =
            assertIs<ProgressComparison.InsufficientData>(
                calculateProgressComparison(listOf(first, second, old), TODAY),
            )
        assertEquals("second", result.series.variantId)
        assertEquals(
            "first",
            assertIs<ProgressComparison.InsufficientData>(
                calculateProgressComparison(listOf(second, first, old), TODAY),
            ).series.variantId,
        )
    }

    @Test
    fun timestampTiesWithinSeriesUseLaterInputForNewestFive() {
        val sessions = (List(5) { 2 } + List(5) { 8 }).map { session(score = it, timestamp = 1) }
        val result = compared(sessions)
        assertEquals(2, result.previousMedian)
        assertEquals(8, result.currentMedian)
        assertEquals(6, result.absoluteChange)
        val reversed = compared(sessions.reversed())
        assertEquals(-6, reversed.absoluteChange)
    }

    @Test
    fun exactKeysSeparateEveryGameModeVariantIncludingNull() {
        val base = GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintClassic, null)
        assertNotEquals(base, base.copy(game = GameId.WideEye))
        assertNotEquals(base, base.copy(mode = GameModeId.NumberSprintBinary))
        assertNotEquals(base, base.copy(variantId = ""))
        assertNotEquals(base.copy(variantId = "a"), base.copy(variantId = "b"))
        val variants =
            listOf(
                session(
                    game = GameId.NumberSprint,
                    mode = GameModeId.NumberSprintCustom,
                    variant = "length:4;digits:01",
                ),
                session(
                    game = GameId.NumberSprint,
                    mode = GameModeId.NumberSprintCustom,
                    variant = "length:6;digits:01",
                ),
                session(game = GameId.WordFlow, mode = GameModeId.WordFlowContext, variant = "ru"),
                session(game = GameId.WordFlow, mode = GameModeId.WordFlowContext, variant = "en"),
                session(variant = null),
                session(variant = "unexpected"),
                session(mode = GameModeId.NumberSprintBinary),
                session(game = GameId.WideEye, mode = GameModeId.WideEyeWords),
            )
        variants.forEach { selected ->
            val other =
                variants.filter { it != selected }.flatMap {
                    item ->
                    List(10) { item.copy(completedAtEpochMillis = 1) }
                }
            val result =
                assertIs<ProgressComparison.InsufficientData>(
                    calculateProgressComparison(other + selected.copy(completedAtEpochMillis = 2), TODAY),
                )
            assertEquals(1, result.availableCount)
            assertEquals(GameSeriesKey(selected.game, selected.mode, selected.variantId), result.series)
        }
    }

    @Test
    fun latestScarceSeriesDoesNotFallBackToOlderCompleteGrowingSeries() {
        val oldSeries = scores(List(5) { 1 } + List(5) { 100 })
        for (count in listOf(1, 7, 9)) {
            val recentSeries = List(count) { session(timestamp = 100L + it, variant = "latest") }
            val result =
                assertIs<ProgressComparison.InsufficientData>(
                    calculateProgressComparison(oldSeries + recentSeries, TODAY),
                )
            assertEquals(count, result.availableCount)
            assertEquals("latest", result.series.variantId)
        }
    }

    @Test
    fun medianIgnoresOutliersAndChronologyDoesNotDependOnInputOrder() {
        val sessions = scores(listOf(1, 2, 2, 2, 100, 3, 4, 4, 4, 100))
        val result = compared(sessions.reversed())
        assertEquals(2, result.previousMedian)
        assertEquals(4, result.currentMedian)
        assertEquals(2, result.absoluteChange)
        assertEquals(10, result.availableCount)
    }

    @Test
    fun onlyLastTenFormDisjointGroupsWhileAvailableCountsAllKnownAttempts() {
        for (olderCount in listOf(1, 8, 1_005)) {
            val sessions = scores(List(olderCount) { 10_000 } + listOf(0, 0, 10, 10, 10, 20, 20, 30, 30, 30))
            val result = compared(sessions)
            assertEquals(olderCount + 10, result.availableCount)
            assertEquals(10, result.previousMedian)
            assertEquals(30, result.currentMedian)
            assertEquals(20, result.absoluteChange)
        }
    }

    @Test
    fun equalDecliningZeroBaseAndMaximumScoresKeepSignedAbsoluteChange() {
        for ((previous, current, expected) in listOf(
            Triple(4, 4, 0),
            Triple(9, 2, -7),
            Triple(0, 0, 0),
            Triple(0, Int.MAX_VALUE, Int.MAX_VALUE),
            Triple(Int.MAX_VALUE, 0, -Int.MAX_VALUE),
        )) {
            val result = compared(scores(List(5) { previous } + List(5) { current }))
            assertEquals(previous, result.previousMedian)
            assertEquals(current, result.currentMedian)
            assertEquals(expected, result.absoluteChange)
        }
    }

    @Test
    fun zeroNonRecordsAndIdenticalAttemptsAreIncludedWithoutMutation() {
        val same = session(score = 0).copy(correctAnswers = 0, durationMillis = 0, isNewRecord = false)
        val input = MutableList(10) { same }
        val before = input.toList()
        val result = compared(input)
        assertEquals(10, result.availableCount)
        assertEquals(0, result.currentMedian)
        assertEquals(0, result.previousMedian)
        assertEquals(result, calculateProgressComparison(input, TODAY))
        assertEquals(before, input)
        assertEquals(
            7,
            assertIs<ProgressComparison.InsufficientData>(
                calculateProgressComparison(input.take(7), TODAY),
            ).availableCount,
        )
    }

    private fun compared(sessions: List<GameSession>) =
        assertIs<ProgressComparison.Compared>(calculateProgressComparison(sessions, TODAY))

    private fun scores(values: List<Int>) =
        values.mapIndexed { index, score ->
            session(
                score,
                timestamp = index.toLong(),
            )
        }

    private fun session(
        score: Int = 0,
        day: Long = TODAY,
        timestamp: Long = 0,
        game: GameId = GameId.NumberSprint,
        mode: GameModeId = GameModeId.NumberSprintClassic,
        variant: String? = null,
    ) = GameSession(
        game = game,
        mode = mode,
        variantId = variant,
        score = score,
        correctAnswers = score,
        durationMillis = 1_000,
        completedAtEpochMillis = timestamp,
        completedEpochDay = day,
        isNewRecord = false,
    )

    private companion object {
        const val TODAY = 20_000L
    }
}
