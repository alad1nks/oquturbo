package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.ui.geometry.Rect
import com.alad1nks.oquturbo.core.data.reminders.ReminderActionObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderActionObservationTest {
    @Test
    fun samplesInitialAndChangedCurrentFieldsWithStableIdentity() {
        val first = mutableListOf<Pair<String, Map<String, String>>>()
        val second = mutableListOf<Pair<String, Map<String, String>>>()
        var sink = ReminderActionObserver { event, fields -> first.add(event to fields) }
        var scroll = mapOf("scrollIndex" to "4", "scrollOffset" to "20", "scrolling" to "true")
        val observation = ReminderActionObservation({ sink }, { scroll })
        observation.bounds = Rect(40f, 450f, 335f, 515f)
        observation.density = 2f
        observation.sampleLayout()
        observation.sampleLayout()
        assertEquals(1, first.size)
        sink = ReminderActionObserver { event, fields -> second.add(event to fields) }
        scroll = mapOf("scrollIndex" to "4", "scrollOffset" to "22", "scrolling" to "false")
        observation.bounds = Rect(40f, 448f, 335f, 513f)
        observation.sampleLayout()
        observation.record("picker-ui-click")
        assertEquals(listOf("picker-ui-layout", "picker-ui-click"), second.map { it.first })
        val fields = second.first().second
        assertEquals("448.0", fields["top"])
        assertEquals("2.0", fields["density"])
        assertEquals("22", fields["scrollOffset"])
        assertEquals("false", fields["scrolling"])
        assertEquals("2", fields["sample"])
        assertTrue(second.all { it.second["observation"] == first.single().second["observation"] })
    }

    @Test
    fun layoutCapEmitsOneMarkerWithoutConsumingInputBudget() {
        val events = mutableListOf<String>()
        val sink = ReminderActionObserver { event, _ -> events.add(event) }
        val observation = ReminderActionObservation({ sink }, { emptyMap() })
        repeat(300) {
            observation.bounds = Rect(0f, it.toFloat(), 20f, it + 20f)
            observation.sampleLayout()
        }
        repeat(140) { observation.record("picker-ui-click") }
        assertEquals(256, events.count { it == "picker-ui-layout" })
        assertEquals(1, events.count { it == "picker-ui-layout-truncated" })
        assertEquals(128, events.count { it == "picker-ui-click" })
    }

    @Test
    fun inputCapDoesNotConsumeLayoutBudgetAndFailuresRemainIsolated() {
        val events = mutableListOf<String>()
        var failSink = false
        var failProvider = false
        val observation =
            ReminderActionObservation(
                { ReminderActionObserver { event, _ -> if (failSink) error("sink") else events.add(event) } },
                { if (failProvider) error("provider") else emptyMap() },
            )
        repeat(128) { observation.record("picker-ui-click") }
        failProvider = true
        observation.sampleLayout()
        failProvider = false
        failSink = true
        observation.sampleLayout()
        failSink = false
        observation.bounds = Rect(0f, 1f, 20f, 21f)
        observation.sampleLayout()
        assertEquals(1, events.count { it == "picker-ui-layout" })
        assertEquals(128, events.count { it == "picker-ui-click" })
    }

    @Test
    fun samplerRunsAt250MillisecondsAndStopsWhenCancelled() =
        runTest {
            val events = mutableListOf<String>()
            var offset = 0
            val observation =
                ReminderActionObservation(
                    { ReminderActionObserver { event, _ -> events.add(event) } },
                    { mapOf("scrollOffset" to offset.toString()) },
                )
            val sampler = launch { observation.sampleLayouts() }
            runCurrent()
            assertEquals(1, events.size)
            offset++
            advanceTimeBy(249)
            runCurrent()
            assertEquals(1, events.size)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(2, events.size)
            advanceTimeBy(500)
            runCurrent()
            assertEquals(2, events.size)
            sampler.cancelAndJoin()
            offset++
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(2, events.size)
        }
}
