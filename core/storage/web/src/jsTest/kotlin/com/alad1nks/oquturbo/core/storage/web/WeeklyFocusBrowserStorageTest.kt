package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.core.storage.web.di.storageWebModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WeeklyFocusBrowserStorageTest {
    @Test
    fun actualNamespaceReloadAndBeforeAfterCommitFailuresRevealPersistedTruth() =
        runTest {
            val namespace = "weekly-focus-browser-test"
            val focusKey = "$namespace:weekly_focus_v1"
            val legacy =
                mapOf(
                    "$namespace:daily_training_v1" to "plan sentinel",
                    "$namespace:game_sessions_v1" to "activity sentinel",
                    "$namespace:daily_training_progress_v1" to "receipt sentinel",
                    "other-weekly-focus-browser-test:weekly_focus_v1" to "other namespace",
                )
            val previous = (legacy.keys + focusKey).associateWith { localStorage.getItem(it) }
            val prototype = js("Storage.prototype")
            val original = prototype.setItem
            val clock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(100L * 86_400_000)
                }
            val application = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
            try {
                localStorage.removeItem(focusKey)
                legacy.forEach { (key, value) -> localStorage.setItem(key, value) }
                val repository = DailyTrainingRepository(application.koin.get<Storage>(), clock)
                assertEquals(WeeklyFocus(), repository.observeWeeklyFocus().first())
                assertNull(localStorage.getItem(focusKey))
                prototype.setItem = { key: dynamic, value: dynamic ->
                    if (key == focusKey) throw IllegalStateException("quota before commit")
                    original.call(localStorage, key, value)
                }
                assertFailsWith<IllegalStateException> { repository.selectWeeklyFocus() }
                assertNull(localStorage.getItem(focusKey))
                prototype.setItem = { key: dynamic, value: dynamic ->
                    original.call(localStorage, key, value)
                    if (key == focusKey) throw IllegalStateException("lost acknowledgement after commit")
                }
                assertFailsWith<IllegalStateException> { repository.selectWeeklyFocus() }
                prototype.setItem = original
                val reopened = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
                try {
                    val reloaded = DailyTrainingRepository(reopened.koin.get<Storage>(), clock)
                    assertEquals(WeeklyFocus(WeeklyFocusSelection(101, 108)), reloaded.observeWeeklyFocus().first())
                    reloaded.disableWeeklyFocus()
                } finally {
                    reopened.close()
                }
                val off = koinApplication { modules(StorageCommonModule, storageWebModule(namespace)) }
                try {
                    assertEquals(
                        WeeklyFocus(),
                        DailyTrainingRepository(off.koin.get(), clock).observeWeeklyFocus().first(),
                    )
                } finally {
                    off.close()
                }
                legacy.forEach { (key, value) -> assertEquals(value, localStorage.getItem(key)) }
            } finally {
                prototype.setItem = original
                application.close()
                previous.forEach {
                    (key, value) ->
                    if (value == null) localStorage.removeItem(key) else localStorage.setItem(key, value)
                }
            }
        }
}
