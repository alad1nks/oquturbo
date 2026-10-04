package com.alad1nks.oquturbo.core.data.practice

import com.alad1nks.oquturbo.core.data.model.DayCompletionStatus
import com.alad1nks.oquturbo.core.data.model.DayHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeRhythmTest {
    @Test
    fun windowCountsZeroThreeFourAndSevenWithoutConfusingGoalWithConsecutiveBadge() {
        for (count in listOf(0, 3, 4, 6, 7)) {
            val rhythm = calculatePracticeRhythm(DayHistory(80, (101L - count..100L).toList()), 100)
            assertEquals((94L..100L).toList(), rhythm.days.map { it.epochDay })
            assertEquals(count, rhythm.completedDaysInWindow)
            assertEquals(count >= 4, rhythm.weeklyGoalReached)
            assertEquals(count >= 7, rhythm.sevenDayStreakEarned)
            assertFalse(rhythm.hasUnknownDays)
        }
        val scattered = calculatePracticeRhythm(DayHistory(80, listOf(94, 96, 98, 100)), 100)
        assertTrue(scattered.weeklyGoalReached)
        assertFalse(scattered.sevenDayStreakEarned)
    }

    @Test
    fun partialStartAndSparsePositiveRemainHonestAndCanStillConfirmTheGoal() {
        val history = DayHistory(97, listOf(94, 97, 98, 100))
        val rhythm = calculatePracticeRhythm(history, 100)
        assertTrue(rhythm.hasUnknownDays)
        assertEquals(4, rhythm.completedDaysInWindow)
        assertTrue(rhythm.weeklyGoalReached)
        assertEquals(DayCompletionStatus.Completed, rhythm.days[0].status)
        assertEquals(DayCompletionStatus.Unknown, rhythm.days[1].status)
        assertEquals(
            DayCompletionStatus.Unknown,
            calculatePracticeRhythm(DayHistory(100, emptyList()), 101).days[5].status,
        )
    }

    @Test
    fun currentSeriesUsesTodayOrYesterdayAndStopsAtFirstUnknownOrKnownGap() {
        data class Case(val start: Long, val facts: List<Long>, val count: Int, val exact: Boolean)
        val cases =
            listOf(
                Case(80, listOf(99, 100), 2, true),
                Case(80, listOf(98, 99), 2, true),
                Case(98, listOf(98, 99), 2, false),
                Case(100, listOf(98, 99), 2, false),
                Case(100, listOf(100), 1, false),
                Case(80, listOf(97), 0, true),
                Case(100, emptyList(), 0, false),
                Case(99, emptyList(), 0, false),
                Case(80, listOf(95, 97, 98, 100), 1, true),
            )
        for (case in cases) {
            val result = calculatePracticeRhythm(DayHistory(case.start, case.facts), 100)
            assertEquals(case.count, result.currentStreakDays, case.toString())
            assertEquals(case.exact, result.currentStreakIsExact, case.toString())
        }
    }

    @Test
    fun bestAndEarnedUseAllConfirmedRunsAndSurviveMissingDaysOrClockRollback() {
        val history = DayHistory(100, (80L..86L).toList() + listOf(82, 103, 102, 101))
        val result = calculatePracticeRhythm(history, 100)
        assertEquals(7, result.bestRecordedStreakDays)
        assertTrue(result.sevenDayStreakEarned)
        assertTrue(result.hasFutureFacts)
        assertEquals(0, result.completedDaysInWindow)
        assertEquals(0, result.currentStreakDays)
        val later = calculatePracticeRhythm(history, 110)
        assertEquals(7, later.bestRecordedStreakDays)
        assertTrue(later.sevenDayStreakEarned)
        assertFalse(later.hasFutureFacts)
    }

    @Test
    fun rolloverChangesCurrentAndWindowButNeverRevokesBestOrEarned() {
        val history = DayHistory(80, (92L..98L).toList())
        val before = calculatePracticeRhythm(history, 99)
        val after = calculatePracticeRhythm(history, 100)
        assertEquals(7, before.currentStreakDays)
        assertEquals(0, after.currentStreakDays)
        assertTrue(after.currentStreakIsExact)
        assertEquals(before.completedDaysInWindow - 1, after.completedDaysInWindow)
        assertEquals(7, after.bestRecordedStreakDays)
        assertTrue(after.sevenDayStreakEarned)
    }

    @Test
    fun futureRunCanProveEarnedButDoesNotCountAsCurrentUntilClockAdvances() {
        val history = DayHistory(80, (101L..107L).toList())
        val now = calculatePracticeRhythm(history, 100)
        assertEquals(0, now.currentStreakDays)
        assertTrue(now.currentStreakIsExact)
        assertEquals(0, now.completedDaysInWindow)
        assertEquals(7, now.bestRecordedStreakDays)
        assertTrue(now.sevenDayStreakEarned)
        val advanced = calculatePracticeRhythm(history, 107)
        assertEquals(7, advanced.currentStreakDays)
        assertEquals(7, advanced.completedDaysInWindow)
        assertFalse(advanced.hasFutureFacts)
    }

    @Test
    fun calendarComponentsCoverEpochLeapDayAndYearBoundary() {
        assertEquals(PracticeDate(1970, 1, 1), practiceDate(0))
        assertEquals(PracticeDate(1969, 12, 31), practiceDate(-1))
        assertEquals(PracticeDate(2000, 2, 29), practiceDate(11_016))
        assertEquals(PracticeDate(2025, 1, 1), practiceDate(20_089))
        assertEquals("0001", PracticeDate(1, 1, 1).yearText)
        assertEquals("-0001", PracticeDate(-1, 1, 1).yearText)
    }
}
