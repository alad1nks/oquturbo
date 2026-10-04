package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderSettingsTest {
    private val schedule = ReminderSchedule(1110, ReminderContent("kk", "Тақырып", "Мәтін"))

    @Test fun strictCodecSeparatesAbsenceFromMalformedAndPreservesUnicode() {
        listOf(null, "", "  ").forEach { assertNull(decodeReminderSchedule(it)) }
        assertEquals(schedule, decodeReminderSchedule(schedule.encodeReminder()))
        for (minute in listOf(
            0,
            1439,
        )) assertEquals(
            minute,
            decodeReminderSchedule(schedule.copy(minutesOfDay = minute).encodeReminder())!!.minutesOfDay,
        )
        val json = schedule.encodeReminder()
        listOf(
            "null",
            "{}",
            "[]",
            "false",
            json.replace("\"version\":1", "\"version\":2"),
            json.replace("\"version\":1", "\"version\":\"1\""),
            json.replace("1110", "1110.0"),
            json.replace("1110", "\"1110\""),
            json.replace("1110", "1440"),
            json.replace("1110", "-1"),
            json.replace("\"kk\"", "\"fr\""),
            json.replace("Тақырып", "  "),
            json.replace("\"body\":\"Мәтін\"", "\"other\":\"Мәтін\""),
        )
            .forEach { assertFails(it) { decodeReminderSchedule(it) } }
        assertEquals(schedule, decodeReminderSchedule(json.replace("\"version\":1", "\"extra\":true,\"version\":1")))
    }

    @Test fun legacyAndOffNeverCreateTimeAndMutationsOnlyTouchTwoReminderKeys() =
        runTest {
            val storage = HistoryTestStorage()
            val repo = SettingsRepository(storage)
            for (desired in listOf(null, false, true)) {
                storage.reminderEnabled.value = desired
                assertEquals(desired ?: false, repo.readRemindersDesired())
                assertNull(repo.readReminderSchedule())
                assertNull(storage.reminderSchedule.value)
            }
            repo.saveReminderSchedule(schedule, enable = true)
            assertEquals(schedule, repo.readReminderSchedule())
            assertTrue(repo.readRemindersDesired())
            repo.setRemindersEnabled(false)
            assertFalse(repo.readRemindersDesired())
            assertEquals(schedule, repo.readReminderSchedule())
            storage.reminderSchedule.value = "corrupt"
            repo.setRemindersEnabled(false)
            assertFalse(repo.readRemindersDesired())
            assertEquals("corrupt", storage.reminderSchedule.value)
            assertEquals(0, storage.activityWrites + storage.planWrites + storage.progressWrites + storage.focusWrites)
        }

    @Test fun twoWritesExposePartialAndPostcommitTruthWithoutCompensation() =
        runTest {
            val storage = HistoryTestStorage()
            val repo = SettingsRepository(storage)
            storage.beforeReminderWrite = { error("before schedule") }
            assertFails { repo.saveReminderSchedule(schedule, true) }
            assertFalse(repo.readRemindersDesired())
            assertNull(storage.reminderSchedule.value)
            storage.beforeReminderWrite = {}
            storage.beforeReminderEnabledWrite = { error("before desire") }
            assertFails { repo.saveReminderSchedule(schedule, true) }
            assertEquals(schedule, repo.readReminderSchedule())
            assertFalse(repo.readRemindersDesired())
            storage.beforeReminderEnabledWrite = {}
            storage.afterReminderEnabledWrite = { error("lost acknowledgement") }
            assertFails { repo.enableSavedReminder() }
            assertTrue(repo.readRemindersDesired())
            storage.afterReminderEnabledWrite = {}
            repo.setRemindersEnabled(false)
            storage.afterReminderWrite = { error("schedule committed") }
            assertFails { repo.saveReminderSchedule(schedule.copy(minutesOfDay = 0), true) }
            assertEquals(0, repo.readReminderSchedule()!!.minutesOfDay)
            assertFalse(repo.readRemindersDesired())
        }

    @Test fun legacySetterAndCompoundSaveShareMutexAndCancellationReleasesIt() =
        runTest {
            val storage = HistoryTestStorage()
            val repo = SettingsRepository(storage)
            val gate = CompletableDeferred<Unit>()
            storage.afterReminderWrite = { gate.await() }
            val save = async { repo.saveReminderSchedule(schedule, true) }
            runCurrent()
            val off = async { repo.setRemindersEnabled(false) }
            runCurrent()
            assertFalse(off.isCompleted)
            gate.complete(Unit)
            save.await()
            off.await()
            assertFalse(repo.readRemindersDesired())
            val blocked = CompletableDeferred<Unit>()
            storage.afterReminderWrite = { blocked.await() }
            val cancelled = launch { repo.saveReminderSchedule(schedule.copy(minutesOfDay = 3), true) }
            runCurrent()
            cancelled.cancelAndJoin()
            repo.setRemindersEnabled(false)
            assertEquals(3, repo.readReminderSchedule()!!.minutesOfDay)
            assertFalse(repo.readRemindersDesired())
        }
}
