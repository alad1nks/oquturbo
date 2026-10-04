package com.alad1nks.oquturbo.core.storage.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderDataStoreTest {
    @Test fun realFileReopenPreservesLegacyDesiredTimeAndUnrelatedValues() =
        runTest {
            val directory = Files.createTempDirectory("reminders-datastore").toFile()
            val file = directory.resolve("reminders.preferences_pb")
            val schedule = ReminderSchedule(0, ReminderContent("ru", "Практика", "Когда удобно"))
            val old =
                mapOf(
                    "weekly_focus_v1" to "focus sentinel",
                    "game_sessions_v1" to "sessions sentinel",
                    "daily_training_v1" to "plan sentinel",
                    "daily_training_progress_v1" to "receipt sentinel",
                )
            try {
                repeat(4) { pass ->
                    val owner = SupervisorJob()
                    val store =
                        PreferenceDataStoreFactory.createWithPath(
                            scope =
                                CoroutineScope(
                                    owner + Dispatchers.IO,
                                ),
                            produceFile = {
                                file.absolutePath.toPath()
                            },
                        )
                    val prefs = AppPreferencesImpl(store)
                    val app =
                        koinApplication { modules(StorageCommonModule, module { single<AppPreferences> { prefs } }) }
                    try {
                        val repo = SettingsRepository(app.koin.get<Storage>())
                        when (pass) {
                            0 -> {
                                old.forEach {
                                    (k, v) ->
                                    prefs.setString(k, v)
                                }
                                prefs.setBoolean("reminders_enabled", true)
                                assertNull(repo.readReminderSchedule())
                            }
                            1 -> {
                                assertTrue(repo.readRemindersDesired())
                                assertNull(repo.readReminderSchedule())
                                repo.saveReminderSchedule(schedule, true)
                            }
                            2 -> {
                                assertTrue(repo.readRemindersDesired())
                                assertEquals(schedule, repo.readReminderSchedule())
                                repo.setRemindersEnabled(false)
                            }
                            3 -> {
                                assertFalse(repo.readRemindersDesired())
                                assertEquals(schedule, repo.readReminderSchedule())
                            }
                        }
                        old.forEach { (k, v) -> assertEquals(v, prefs.getString(k).first()) }
                    } finally {
                        app.close()
                        owner.cancelAndJoin()
                    }
                }
            } finally {
                directory.deleteRecursively()
            }
        }
}
