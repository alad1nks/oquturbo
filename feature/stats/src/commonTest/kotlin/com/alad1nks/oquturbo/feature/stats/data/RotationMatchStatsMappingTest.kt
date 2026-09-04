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

class RotationMatchStatsMappingTest {
    @Test
    fun rotationSeriesMapsEverySurfaceAndOnlyContributesToVisualPerception() {
        val session =
            GameSession(
                game = GameId.RotationMatch,
                mode = GameModeId.RotationMatchRotation,
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
                records = listOf(GameRecord(GameId.RotationMatch, GameModeId.RotationMatchRotation, score = 6)),
                totals =
                    GameActivityTotals(
                        sessionCount = 1,
                        durationMillis = 4_000,
                        correctAnswers = 6,
                        series =
                            listOf(
                                GameSeriesTotals(
                                    game = GameId.RotationMatch,
                                    mode = GameModeId.RotationMatchRotation,
                                    sessionCount = 1,
                                    durationMillis = 4_000,
                                    correctAnswers = 6,
                                    scoreTotal = 6,
                                ),
                            ),
                    ),
                todayEpochDay = 0,
            )

        val trend = snapshot.trends.single { it.game == StatsGame.RotationMatch }
        assertEquals(listOf(StatsMode.Rotation), trend.modes.map { it.mode })
        assertEquals(1, snapshot.skills.single { it.skill == StatsSkill.VisualPerception }.trainings)
        assertEquals(
            0,
            snapshot.skills.filter { it.skill != StatsSkill.VisualPerception }.sumOf { it.trainings },
        )
        assertEquals(StatsGame.RotationMatch, snapshot.recentActivity.single().game)
        assertEquals(StatsMode.Rotation, snapshot.recentActivity.single().mode)
        assertEquals(1, snapshot.games.single { it.game == StatsGame.RotationMatch }.gamesPlayed)
    }
}
