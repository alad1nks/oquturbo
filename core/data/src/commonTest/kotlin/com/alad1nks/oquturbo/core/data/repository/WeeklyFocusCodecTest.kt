package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusPhase
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WeeklyFocusCodecTest {
    @Test
    fun absenceIsOffButJsonMustExplicitlyDeclareVersionAndSelection() {
        for (value in listOf(null, "", "  ", """{"version":1,"selection":null}""")) {
            assertEquals(WeeklyFocus(), decodeWeeklyFocus(value))
        }
        for (value in listOf(
            "null",
            "{}",
            "[]",
            "true",
            "{",
            """{"version":1}""",
            """{"selection":null}""",
            """{"version":"1","selection":null}""",
            """{"version":1.0,"selection":null}""",
            """{"version":2,"selection":null}""",
            """{"version":1,"selection":false}""",
            """{"version":1,"selection":{}}""",
        )) {
            assertFails(value) { decodeWeeklyFocus(value) }
        }
    }

    @Test
    fun selectionRequiresExactKindIntegerDatesAndCheckedSevenDayInterval() {
        val valid = """{"version":1,"selection":{"kind":"number_sprint_classic",
            "startEpochDay":101,"endExclusiveEpochDay":108}}"""
        assertEquals(WeeklyFocus(WeeklyFocusSelection(101, 108)), decodeWeeklyFocus(valid))
        for (value in listOf(
            valid.replace("number_sprint_classic", "other"),
            valid.replace("\"kind\":\"number_sprint_classic\",", ""),
            valid.replace(":101", ":\"101\""),
            valid.replace(":101", ":101.0"),
            valid.replace(":101", ":true"),
            valid.replace(":101", ":null"),
            valid.replace("\"startEpochDay\":101,", ""),
            valid.replace(":108", ":107"),
            valid.replace(":108", ":109"),
            valid.replace(":108", ":9223372036854775808"),
        )) {
            assertFails(value) { decodeWeeklyFocus(value) }
        }
        assertEquals(
            decodeWeeklyFocus(valid),
            decodeWeeklyFocus(valid.replace("\"version\":1", "\"extra\":true,\"version\":1")),
        )
    }

    @Test
    fun extremePastFutureIntervalsAreValidWithoutArtificialWriterBounds() {
        for (start in listOf(Long.MIN_VALUE, -106_751_991_174L, -1L, 0L, 106_751_991_168L, Long.MAX_VALUE - 7)) {
            val setting = WeeklyFocus(WeeklyFocusSelection(start, start + 7))
            assertEquals(setting, decodeWeeklyFocus(setting.encodeFocus()))
            assertEquals(start + 6, setting.selection!!.lastEpochDay)
        }
        for (start in Long.MAX_VALUE - 6..Long.MAX_VALUE) {
            assertFails { WeeklyFocusSelection(start, start + 7) }
        }
    }

    @Test
    fun phaseUsesOrderedBoundariesAndClockRollbackNeverRecreatesAnOffSelection() {
        val focus = WeeklyFocus(WeeklyFocusSelection.after(100))
        assertEquals(WeeklyFocusPhase.Scheduled, focus.phaseOn(100))
        assertEquals(WeeklyFocusPhase.Active, focus.phaseOn(101))
        assertEquals(WeeklyFocusPhase.Active, focus.phaseOn(107))
        assertEquals(WeeklyFocusPhase.Expired, focus.phaseOn(108))
        assertEquals(WeeklyFocusPhase.Scheduled, focus.phaseOn(Long.MIN_VALUE))
        assertEquals(WeeklyFocusPhase.Expired, focus.phaseOn(Long.MAX_VALUE))
        assertEquals(WeeklyFocusPhase.Active, focus.phaseOn(104))
        assertEquals(WeeklyFocusPhase.Off, WeeklyFocus().phaseOn(104))
        assertEquals(
            WeeklyFocusSelection(Long.MAX_VALUE - 7, Long.MAX_VALUE),
            WeeklyFocusSelection.after(Long.MAX_VALUE - 8),
        )
        assertFails { WeeklyFocusSelection.after(Long.MAX_VALUE - 7) }
        assertFails { WeeklyFocusSelection.after(Long.MAX_VALUE) }
    }
}
