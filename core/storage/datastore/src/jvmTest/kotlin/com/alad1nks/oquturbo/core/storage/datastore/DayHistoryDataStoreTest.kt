package com.alad1nks.oquturbo.core.storage.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DayHistoryDataStoreTest {
    @Test
    fun actualDataStoreReopenPreservesCombinedSessionsAndFlatTrainingReceipts() =
        runTest {
            val directory = Files.createTempDirectory("day-history-datastore").toFile()
            val file = directory.resolve("history.preferences_pb")
            val clock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(100L * 86_400_000L)
                }
            var activityBytes: String? = null
            var progressBytes: String? = null
            try {
                repeat(2) { reopen ->
                    val owner = SupervisorJob()
                    val dataStore =
                        PreferenceDataStoreFactory.createWithPath(
                            scope = CoroutineScope(owner + Dispatchers.IO),
                            produceFile = { file.absolutePath.toPath() },
                        )
                    val application =
                        koinApplication {
                            modules(
                                StorageCommonModule,
                                module { single<AppPreferences> { AppPreferencesImpl(dataStore) } },
                            )
                        }
                    try {
                        val storage = application.koin.get<Storage>()
                        val activity = GameActivityRepository(storage, clock)
                        val training = DailyTrainingRepository(storage, clock)
                        if (reopen == 0) {
                            activity.recordCompletedSession(
                                GameId.WordFlow,
                                GameModeId.WordFlowContext,
                                "ru",
                                score = 0,
                                correctAnswers = 0,
                                durationMillis = 1,
                                isNewRecord = false,
                            )
                            var plan = training.ensureTodayTraining()
                            while (!plan.isCompleted) plan = training.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE)
                            activityBytes = storage.getGameSessionsJson().first()
                            progressBytes = storage.getDailyTrainingProgressJson().first()
                            assertTrue(file.isFile)
                        } else {
                            assertEquals(activityBytes, storage.getGameSessionsJson().first())
                            assertEquals(progressBytes, storage.getDailyTrainingProgressJson().first())
                        }
                        assertEquals(setOf(100L), activity.observePracticeHistory().first().completedEpochDays)
                        assertEquals(setOf(100L), training.observeCompletionHistory().first().completedEpochDays)
                        assertEquals(1, activity.observeSessions().first().size)
                        assertEquals(0, activity.observeProgress().first().totalXp)
                        assertEquals(1, training.observeProgress().first().totalCompletedTrainings)
                        assertTrue(training.ensureTodayTraining().isCompleted)
                    } finally {
                        application.close()
                        owner.cancelAndJoin() // release the actual file before constructing a new DataStore
                    }
                }
            } finally {
                directory.deleteRecursively()
            }
        }
}
