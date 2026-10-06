package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.core.data.model.GameActivityTotals
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsSkill
import com.alad1nks.oquturbo.feature.stats.model.StatsTrend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class StatsComparisonRegressionTest {
    @Test
    fun periodComparisonStillUsesMeansRatherThanMedians() {
        val snapshot = snapshot(group(listOf(1, 1, 10), previous = true) + group(listOf(2, 2, 8)))
        val mode = snapshot.trends.single().modes.single()
        assertEquals(4, mode.averageResult)
        assertEquals(0, mode.comparisonPercent)
        assertEquals(0, snapshot.skills.single { it.skill == StatsSkill.Memory }.averageChangePercent)
    }

    @Test
    fun minimumThreeAppliesToBothGroupsAndZeroPreviousMeanHasNoPercentage() {
        for ((previous, current) in listOf(
            listOf(10, 10) to listOf(20, 20, 20),
            listOf(10, 10, 10) to listOf(20, 20),
            listOf(0, 0, 0) to listOf(20, 20, 20),
        )) {
            val snapshot = snapshot(group(previous, previous = true) + group(current))
            assertNull(snapshot.trends.single().modes.single().comparisonPercent)
            val skill = snapshot.skills.single { it.skill == StatsSkill.Memory }
            assertNull(skill.averageChangePercent)
            assertEquals(StatsTrend.NotEnoughData, skill.trend)
        }
    }

    @Test
    fun positiveNegativeAndEqualMeansKeepTheirPercentageAndTrend() {
        for ((currentScore, percentage, trend) in listOf(
            Triple(15, 50, StatsTrend.Growing),
            Triple(5, -50, StatsTrend.Declining),
            Triple(10, 0, StatsTrend.Stable),
        )) {
            val snapshot = snapshot(group(List(3) { 10 }, previous = true) + group(List(3) { currentScore }))
            assertEquals(percentage, snapshot.trends.single().modes.single().comparisonPercent)
            val skill = snapshot.skills.single { it.skill == StatsSkill.Memory }
            assertEquals(percentage, skill.averageChangePercent)
            assertEquals(trend, skill.trend)
        }
    }

    @Test
    fun skillComparisonWeightsOnlyComparableGroupsByCurrentCount() {
        val classic = group(List(3) { 10 }, previous = true) + group(List(3) { 20 })
        val binary =
            group(List(3) { 10 }, previous = true, mode = GameModeId.NumberSprintBinary) +
                group(List(6) { 5 }, mode = GameModeId.NumberSprintBinary)
        val insufficient =
            group(List(2) { 1 }, previous = true, mode = GameModeId.NumberSprintCustom, variant = "tiny") +
                group(List(30) { 100 }, mode = GameModeId.NumberSprintCustom, variant = "tiny")
        val snapshot = snapshot(classic + binary + insufficient)
        val modes = snapshot.trends.single().modes.associateBy { it.mode }
        assertEquals(100, modes.getValue(StatsMode.Classic).comparisonPercent)
        assertEquals(-50, modes.getValue(StatsMode.Binary).comparisonPercent)
        assertNull(modes.getValue(StatsMode.Custom).comparisonPercent)
        val skill = snapshot.skills.single { it.skill == StatsSkill.Memory }
        // (100 * 3 + -50 * 6) / 9, excluding the non-comparable custom group.
        assertEquals(0, skill.averageChangePercent)
        assertEquals(StatsTrend.Stable, skill.trend)
    }

    @Test
    fun customConfigurationsAndWordFlowLanguagesRemainSeparateInWeightedComparisons() {
        for ((game, mode, skill) in listOf(
            Triple(GameId.NumberSprint, GameModeId.NumberSprintCustom, StatsSkill.Memory),
            Triple(GameId.WordFlow, GameModeId.WordFlowContext, StatsSkill.Reading),
        )) {
            val sessions =
                group(List(3) { 10 }, true, game, mode, "a") +
                    group(List(3) { 20 }, false, game, mode, "a") +
                    group(List(3) { 100 }, true, game, mode, "b") +
                    group(List(3) { 50 }, false, game, mode, "b")
            val snapshot = snapshot(sessions)
            val modes = snapshot.trends.single().modes.associateBy { it.variantId }
            assertEquals(setOf("a", "b"), modes.keys)
            assertEquals(100, modes.getValue("a").comparisonPercent)
            assertEquals(-50, modes.getValue("b").comparisonPercent)
            assertEquals(25, snapshot.skills.single { it.skill == skill }.averageChangePercent)

            val unmatched =
                snapshot(
                    group(List(3) { 10 }, true, game, mode, null) +
                        group(List(3) { 20 }, false, game, mode, "new"),
                )
            assertNull(unmatched.trends.single().modes.single().comparisonPercent)
            assertNull(unmatched.skills.single { it.skill == skill }.averageChangePercent)
        }
    }

    @Test
    fun unexpectedVariantsOfOrdinaryModesStillNormalizeToOneStatsSeries() {
        val sessions =
            group(List(3) { 10 }, previous = true, variant = "old") +
                group(List(3) { 20 }, variant = "new")
        val snapshot = snapshot(sessions)
        val mode = snapshot.trends.single().modes.single()
        assertNull(mode.variantId)
        assertEquals(100, mode.comparisonPercent)
        assertEquals(100, snapshot.skills.single { it.skill == StatsSkill.Memory }.averageChangePercent)
    }

    @Test
    fun allTimeKeepsComparisonsHidden() {
        val snapshot = snapshot(group(List(3) { 10 }, previous = true) + group(List(3) { 20 }), StatsPeriod.AllTime)
        assertNull(snapshot.trends.single().modes.single().comparisonPercent)
        snapshot.skills.forEach {
            assertNull(it.averageChangePercent)
            assertFalse(it.showComparison)
        }
    }

    private fun snapshot(sessions: List<GameSession>, period: StatsPeriod = StatsPeriod.SevenDays) =
        createSnapshot(
            period = period,
            sessions = sessions,
            records = emptyList(),
            totals = GameActivityTotals(sessionCount = sessions.size.toLong()),
            todayEpochDay = 100,
        )

    private fun group(
        scores: List<Int>,
        previous: Boolean = false,
        game: GameId = GameId.NumberSprint,
        mode: GameModeId = GameModeId.NumberSprintClassic,
        variant: String? = null,
    ): List<GameSession> =
        scores.mapIndexed { index, score ->
            val day = if (previous) 90L else 100L
            GameSession(
                game = game,
                mode = mode,
                variantId = variant,
                score = score,
                correctAnswers = score,
                durationMillis = 1_000,
                completedAtEpochMillis = day * 86_400_000L + index,
                completedEpochDay = day,
                isNewRecord = false,
            )
        }
}
