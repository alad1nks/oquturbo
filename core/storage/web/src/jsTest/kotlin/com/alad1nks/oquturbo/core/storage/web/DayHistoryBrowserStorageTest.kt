package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.core.storage.web.di.storageWebModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class DayHistoryBrowserStorageTest {
    @Test
    fun realLocalStorageFailurePublishesNeitherSessionNorHistoryAndReloadReadsCommittedFacts() =
        runTest {
            val namespace = "practice-history-browser-test"
            val keys = listOf("game_sessions_v1", "daily_training_progress_v1", "daily_training_v1")
            val previous = keys.associateWith { localStorage.getItem("$namespace:$it") }
            keys.forEach { localStorage.removeItem("$namespace:$it") }
            val clock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(100L * 86_400_000L)
                }
            val prototype = js("Storage.prototype")
            val original = prototype.setItem
            val application = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
            try {
                val storage = application.koin.get<Storage>()
                val activity = GameActivityRepository(storage, clock)
                val training = DailyTrainingRepository(storage, clock)
                val observed = mutableListOf<DayHistory>()
                val collecting =
                    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                        activity.observePracticeHistory().collect { observed.add(it) }
                    }
                val before = localStorage.getItem("$namespace:game_sessions_v1")
                prototype.setItem = { _: dynamic, _: dynamic -> throw IllegalStateException("quota exceeded") }
                assertFailsWith<IllegalStateException> {
                    activity.recordCompletedSession(
                        GameId.WideEye,
                        GameModeId.WideEyeWords,
                        score = 5,
                        durationMillis = 1,
                        isNewRecord = false,
                    )
                }
                runCurrent()
                assertEquals(before, localStorage.getItem("$namespace:game_sessions_v1"))
                assertEquals(listOf(DayHistory(100, emptyList())), observed)
                assertTrue(activity.observeSessions().first().isEmpty())
                prototype.setItem = original
                activity.recordCompletedSession(
                    GameId.WideEye,
                    GameModeId.WideEyeWords,
                    score = 5,
                    durationMillis = 1,
                    isNewRecord = false,
                )
                var plan = training.ensureTodayTraining()
                while (!plan.isCompleted) plan = training.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE)
                collecting.cancel()
                val persisted = keys.associateWith { localStorage.getItem("$namespace:$it") }
                val reopened = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
                try {
                    val reloadedStorage = reopened.koin.get<Storage>()
                    val reloadedActivity = GameActivityRepository(reloadedStorage, clock)
                    val reloadedTraining = DailyTrainingRepository(reloadedStorage, clock)
                    assertEquals(setOf(100L), reloadedActivity.observePracticeHistory().first().completedEpochDays)
                    assertEquals(setOf(100L), reloadedTraining.observeCompletionHistory().first().completedEpochDays)
                    assertEquals(5, reloadedActivity.observeProgress().first().totalXp)
                    assertEquals(1, reloadedTraining.observeProgress().first().totalCompletedTrainings)
                    assertEquals(persisted, keys.associateWith { localStorage.getItem("$namespace:$it") })
                } finally {
                    reopened.close()
                }
            } finally {
                prototype.setItem = original
                application.close()
                previous.forEach { (key, value) ->
                    if (value == null) {
                        localStorage.removeItem("$namespace:$key")
                    } else {
                        localStorage.setItem("$namespace:$key", value)
                    }
                }
            }
        }
}
