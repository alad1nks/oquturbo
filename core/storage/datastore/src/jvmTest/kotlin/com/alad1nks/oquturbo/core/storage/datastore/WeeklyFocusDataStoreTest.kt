package com.alad1nks.oquturbo.core.storage.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
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
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WeeklyFocusDataStoreTest {
    @Test
    fun selectAndDisableSurviveRealFileReopenWithoutTouchingExistingPayloads() =
        runTest {
            val directory = Files.createTempDirectory("weekly-focus-datastore").toFile()
            val file = directory.resolve("focus.preferences_pb")
            val clock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(100L * 86_400_000)
                }
            val expected = WeeklyFocus(WeeklyFocusSelection(101, 108))
            val legacy =
                mapOf(
                    "game_sessions_v1" to "activity sentinel",
                    "daily_training_v1" to "plan sentinel",
                    "daily_training_progress_v1" to "progress sentinel",
                    "profile_preferences_v1" to "profile sentinel",
                )
            try {
                repeat(3) { pass ->
                    val owner = SupervisorJob()
                    val dataStore =
                        PreferenceDataStoreFactory.createWithPath(
                            scope = CoroutineScope(owner + Dispatchers.IO),
                            produceFile = { file.absolutePath.toPath() },
                        )
                    val preferences = AppPreferencesImpl(dataStore)
                    val application =
                        koinApplication {
                            modules(
                                StorageCommonModule,
                                module { single<AppPreferences> { preferences } },
                            )
                        }
                    try {
                        val storage = application.koin.get<Storage>()
                        val repository = DailyTrainingRepository(storage, clock)
                        when (pass) {
                            0 -> {
                                legacy.forEach { (key, value) -> preferences.setString(key, value) }
                                assertEquals(WeeklyFocus(), repository.observeWeeklyFocus().first())
                                assertNull(storage.getWeeklyFocusJson().first())
                                assertEquals(expected, repository.selectWeeklyFocus())
                            }
                            1 -> {
                                assertEquals(expected, repository.observeWeeklyFocus().first())
                                repository.disableWeeklyFocus()
                            }
                            2 -> assertEquals(WeeklyFocus(), repository.observeWeeklyFocus().first())
                        }
                        legacy.forEach { (key, value) -> assertEquals(value, preferences.getString(key).first()) }
                    } finally {
                        application.close()
                        owner.cancelAndJoin()
                    }
                }
            } finally {
                directory.deleteRecursively()
            }
        }
}
