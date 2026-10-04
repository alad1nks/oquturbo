package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DayHistoryDateRangeTest {
    @Test
    fun exactEpochMillisecondWriterEdgesAreValidForStartAndFactsAndSurviveActivityWrites() =
        runTest {
            for (start in listOf(MINIMUM, MAXIMUM)) {
                val storage = storageWith(metadata(start, "$MINIMUM,$MAXIMUM"))
                val activity = GameActivityRepository(storage, HistoryClock())
                val training = DailyTrainingRepository(storage, HistoryClock())
                assertEquals(start, activity.observePracticeHistory().first().trackingStartedEpochDay)
                assertEquals(setOf(MINIMUM, MAXIMUM), training.observeCompletionHistory().first().completedEpochDays)
                activity.recordCompletedSession(
                    GameId.NumberSprint,
                    GameModeId.NumberSprintBinary,
                    score = 0,
                    durationMillis = 0,
                    isNewRecord = false,
                )
                assertEquals(start, activity.observePracticeHistory().first().trackingStartedEpochDay)
                assertEquals(
                    setOf(MINIMUM, 100L, MAXIMUM),
                    activity.observePracticeHistory().first().completedEpochDays,
                )
                assertEquals(42, activity.observeProgress().first().totalXp)
            }
        }

    @Test
    fun unrepresentableStartsAndAnyUnrepresentableFactRejectBothSourcesWithoutMutation() =
        runTest {
            for (invalid in listOf(MINIMUM - 1, MAXIMUM + 1, Long.MIN_VALUE, Long.MAX_VALUE)) {
                for (history in listOf(metadata(invalid, "100"), metadata(100, "100,$invalid"))) {
                    val storage = storageWith(history)
                    val beforeActivity = storage.activity.value
                    val beforeProgress = storage.progress.value
                    val activity = GameActivityRepository(storage, HistoryClock())
                    val training = DailyTrainingRepository(storage, HistoryClock())
                    assertEquals(42, activity.observeProgress().first().totalXp)
                    assertEquals(8, training.observeProgress().first().totalCompletedTrainings)
                    assertFailsWith<IllegalStateException> { activity.observePracticeHistory().first() }
                    assertFailsWith<IllegalStateException> { training.observeCompletionHistory().first() }
                    assertFailsWith<IllegalStateException> {
                        activity.recordCompletedSession(
                            GameId.NumberSprint,
                            GameModeId.NumberSprintBinary,
                            score = 1,
                            durationMillis = 1,
                            isNewRecord = false,
                        )
                    }
                    assertFailsWith<IllegalStateException> { training.completeEntry("arbitrary", 100) }
                    assertEquals(0, storage.activityWrites + storage.progressWrites + storage.planWrites)
                    assertEquals(beforeActivity, storage.activity.value)
                    assertEquals(beforeProgress, storage.progress.value)
                }
            }
        }

    private fun metadata(start: Long, days: String): String =
        """{"version":1,"trackingStartedEpochDay":$start,"completedEpochDays":[$days]}"""

    private fun storageWith(history: String): HistoryTestStorage =
        HistoryTestStorage().apply {
            activity.value = """{"version":1,"totalCorrectAnswers":42,"practiceHistory":$history}"""
            progress.value = """{"version":1,"totalCompletedTrainings":8,"lastCompletedEpochDay":99,
            "completionHistory":$history}"""
        }

    private companion object {
        const val MINIMUM = Long.MIN_VALUE / 86_400_000L
        const val MAXIMUM = Long.MAX_VALUE / 86_400_000L
    }
}
