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
}
