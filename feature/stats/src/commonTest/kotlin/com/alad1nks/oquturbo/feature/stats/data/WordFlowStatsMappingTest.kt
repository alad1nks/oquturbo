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

class WordFlowStatsMappingTest {
    @Test
    fun localeVariantsRemainSeparateAndReadingUsesOnlyWordFlow() {
        val sessions =
            listOf(
                session("en", score = 4, completedAt = 1),
                session("ru", score = 7, completedAt = 2),
            )
        val totals =
            GameActivityTotals(
                sessionCount = 2,
                durationMillis = 2_000,
                correctAnswers = 11,
                series =
                    sessions.map { session ->
                        GameSeriesTotals(
                            game = session.game,
                            mode = session.mode,
                            variantId = session.variantId,
                            sessionCount = 1,
                            durationMillis = session.durationMillis,
                            correctAnswers = session.correctAnswers.toLong(),
                            scoreTotal = session.score.toLong(),
                        )
                    },
            )
        val snapshot =
            createSnapshot(
                period = StatsPeriod.AllTime,
                sessions = sessions,
                records =
                    listOf(
                        GameRecord(GameId.WordFlow, GameModeId.WordFlowContext, "en", 4),
                        GameRecord(GameId.WordFlow, GameModeId.WordFlowContext, "ru", 7),
                    ),
                totals = totals,
                todayEpochDay = 0,
            )

        val trend = snapshot.trends.single { it.game == StatsGame.WordFlow }
        assertEquals(setOf("en", "ru"), trend.modes.map { it.variantId }.toSet())
        assertEquals(setOf(StatsMode.Context), trend.modes.map { it.mode }.toSet())
        assertEquals(2, snapshot.skills.single { it.skill == StatsSkill.Reading }.trainings)
        assertEquals(setOf("en", "ru"), snapshot.recentActivity.mapNotNull { it.variantId }.toSet())
    }

    private fun session(
        locale: String,
        score: Int,
        completedAt: Long,
    ) = GameSession(
        game = GameId.WordFlow,
        mode = GameModeId.WordFlowContext,
        variantId = locale,
        score = score,
        correctAnswers = score,
        durationMillis = 1_000,
        completedAtEpochMillis = completedAt,
        completedEpochDay = 0,
        isNewRecord = true,
    )
}
