package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.core.data.model.GameActivityTotals
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameRecord
import com.alad1nks.oquturbo.core.data.model.GameSeriesTotals
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsSkill
import kotlin.test.Test
import kotlin.test.assertEquals

class DualFocusStatsMappingTest {
    @Test
    fun matchSeriesMapsToDualFocusAndContributesToAttention() {
        val session =
            GameSession(
                game = GameId.DualFocus,
                mode = GameModeId.DualFocusMatch,
                score = 6,
                correctAnswers = 6,
                durationMillis = 4_000,
                completedAtEpochMillis = 1,
                completedEpochDay = 0,
                isNewRecord = true,
            )
        val snapshot =
            createSnapshot(
                period = StatsPeriod.AllTime,
                sessions = listOf(session),
                records = listOf(GameRecord(GameId.DualFocus, GameModeId.DualFocusMatch, score = 6)),
                totals =
                    GameActivityTotals(
                        sessionCount = 1,
                        durationMillis = 4_000,
                        correctAnswers = 6,
                        series =
                            listOf(
                                GameSeriesTotals(
                                    game = GameId.DualFocus,
                                    mode = GameModeId.DualFocusMatch,
                                    sessionCount = 1,
                                    durationMillis = 4_000,
                                    correctAnswers = 6,
                                    scoreTotal = 6,
                                ),
                            ),
                    ),
                todayEpochDay = 0,
            )

        val trend = snapshot.trends.single { it.game == StatsGame.DualFocus }
        assertEquals(listOf(StatsMode.Match), trend.modes.map { it.mode })
        assertEquals(1, snapshot.skills.single { it.skill == StatsSkill.Attention }.trainings)
        assertEquals(StatsGame.DualFocus, snapshot.recentActivity.single().game)
    }
}
