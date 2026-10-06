package com.alad1nks.oquturbo.core.data.practice

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PracticeDateRangeTest {
    @Test
    fun fullWriterRangeAndAdjacentWeekDaysMatchIndependentJavaCalendar() {
        val edge = Long.MAX_VALUE / 86_400_000L
        for (day in listOf(-edge - 6, -edge, -2_147_483_648L, -719_528L, -1, 0, 20_089, 2_147_483_648L, edge)) {
            val expected = LocalDate.ofEpochDay(day)
            val result = practiceDate(day)
            assertEquals(expected.year.toLong(), result.year, "epoch day $day")
            assertEquals(expected.monthValue, result.month)
            assertEquals(expected.dayOfMonth, result.day)
        }
    }

    @Test
    fun fullJavaOverlapAndWriterPreviewExtensionMatchIndependentCalendar() {
        val points =
            listOf(
                -365_243_219_162L,
                365_241_780_471L,
                -106_751_991_194L,
                106_751_991_174L,
                -719_528L,
                -1L,
                0L,
                10_957L,
                11_016L,
                47_540L,
            )
        for (day in points + (-146_097L..146_097L step 997)) {
            val expected = LocalDate.ofEpochDay(day)
            assertEquals(
                PracticeDate(expected.year.toLong(), expected.monthValue, expected.dayOfMonth),
                practiceDate(day),
            )
        }
    }

    @Test
    fun longEdgesMatchIndependent400YearDatetimeOracleWithoutOverflow() {
        val fixtures =
            mapOf(
                Long.MIN_VALUE to PracticeDate(-25_252_734_927_764_585L, 6, 7),
                Long.MIN_VALUE + 1 to PracticeDate(-25_252_734_927_764_585L, 6, 8),
                Long.MIN_VALUE + 6 to PracticeDate(-25_252_734_927_764_585L, 6, 13),
                Long.MIN_VALUE + 7 to PracticeDate(-25_252_734_927_764_585L, 6, 14),
                Long.MAX_VALUE - 8 to PracticeDate(25_252_734_927_768_524L, 7, 19),
                Long.MAX_VALUE - 7 to PracticeDate(25_252_734_927_768_524L, 7, 20),
                Long.MAX_VALUE - 6 to PracticeDate(25_252_734_927_768_524L, 7, 21),
                Long.MAX_VALUE - 1 to PracticeDate(25_252_734_927_768_524L, 7, 26),
                Long.MAX_VALUE to PracticeDate(25_252_734_927_768_524L, 7, 27),
                Long.MAX_VALUE - 719_468 to PracticeDate(25_252_734_927_766_554L, 9, 25),
                Long.MAX_VALUE - 719_467 to PracticeDate(25_252_734_927_766_554L, 9, 26),
            )
        fixtures.forEach { (epoch, expected) -> assertEquals(expected, practiceDate(epoch), "epoch $epoch") }
        assertEquals("-25252734927764585", practiceDate(Long.MIN_VALUE).yearText)
        assertEquals("25252734927768524", practiceDate(Long.MAX_VALUE).yearText)
    }
}
