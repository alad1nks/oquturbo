package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.core.storage.web.di.storageWebModule
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderBrowserStorageTest {
    @Test fun realNamespaceTwoKeyFailureAndReloadPreserveTruth() =
        runTest {
            val namespace = "reminder-browser-test"
            val scheduleKey = "$namespace:reminders_schedule_v1"
            val desiredKey = "$namespace:reminders_enabled"
            val legacy =
                mapOf(
                    "$namespace:weekly_focus_v1" to "focus",
                    "$namespace:game_sessions_v1" to "sessions",
                    "other-reminder-test:reminders_enabled" to "true",
                )
            val previous = (legacy.keys + scheduleKey + desiredKey).associateWith { localStorage.getItem(it) }
            val prototype = js("Storage.prototype")
            val original = prototype.setItem
            val app = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
            val schedule = ReminderSchedule(1439, ReminderContent("kk", "Жаттығу", "Ыңғайлы кезде"))
            try {
                localStorage.removeItem(scheduleKey)
                localStorage.removeItem(desiredKey)
                legacy.forEach { (k, v) -> localStorage.setItem(k, v) }
                val repo = SettingsRepository(app.koin.get<Storage>())
                assertFalse(repo.readRemindersDesired())
                assertNull(repo.readReminderSchedule())
                prototype.setItem = { key: dynamic, value: dynamic ->
                    if (key == desiredKey)throw IllegalStateException("before desired commit")
                    original.call(localStorage, key, value)
                }
                assertFails { repo.saveReminderSchedule(schedule, true) }
                assertEquals(schedule, repo.readReminderSchedule())
                assertFalse(repo.readRemindersDesired())
                prototype.setItem = { key: dynamic, value: dynamic ->
                    original.call(localStorage, key, value)
                    if (key == desiredKey)throw IllegalStateException("after desired commit")
                }
                assertFails { repo.enableSavedReminder() }
                prototype.setItem = original
                val reopened = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
                try {
                    val fresh = SettingsRepository(reopened.koin.get<Storage>())
                    assertTrue(fresh.readRemindersDesired())
                    assertEquals(schedule, fresh.readReminderSchedule())
                    fresh.setRemindersEnabled(false)
                    assertFalse(fresh.readRemindersDesired())
                    assertEquals(schedule, fresh.readReminderSchedule())
                } finally {
                    reopened.close()
                }
                legacy.forEach { (k, v) -> assertEquals(v, localStorage.getItem(k)) }
            } finally {
                prototype.setItem = original
                app.close()
                previous.forEach { (k, v) -> if (v == null)localStorage.removeItem(k)else localStorage.setItem(k, v) }
            }
        }
}
