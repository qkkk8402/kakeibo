package com.example.kakeibo

import com.example.kakeibo.model.MonthlySummary
import com.example.kakeibo.model.datePickerMillisToLocalDate
import com.example.kakeibo.model.localDateToDatePickerMillis
import com.example.kakeibo.model.monthRange
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthlySummaryTest {
    @Test
    fun calculatesBalanceAndRemainingBudget() {
        val summary = MonthlySummary(YearMonth.of(2026, 8), income = 250_000, expense = 65_000, budget = 100_000)

        assertEquals(185_000L, summary.balance)
        assertEquals(35_000L, summary.remainingBudget)
        assertEquals(0.65f, summary.usageRatio!!, 0.001f)
    }

    @Test
    fun reportsOverBudgetAsNegativeRemaining() {
        val summary = MonthlySummary(YearMonth.of(2026, 8), expense = 120_000, budget = 100_000)

        assertEquals(-20_000L, summary.remainingBudget)
        assertEquals(1.2f, summary.usageRatio!!, 0.001f)
    }

    @Test
    fun budgetNotSetDoesNotHaveUsage() {
        val summary = MonthlySummary(YearMonth.of(2026, 8), expense = 10_000)

        assertNull(summary.remainingBudget)
        assertNull(summary.usageRatio)
    }

    @Test
    fun monthRangeUsesNextMonthAsExclusiveBoundary() {
        val range = monthRange(YearMonth.of(2024, 2))

        assertEquals(29, range.count())
        assertEquals(YearMonth.of(2024, 2).atDay(1).toEpochDay(), range.first)
        assertEquals(YearMonth.of(2024, 3).atDay(1).toEpochDay() - 1, range.last)
    }

    @Test
    fun datePickerRoundTripUsesCalendarDateInUtc() {
        val date = LocalDate.of(2026, 8, 4)

        val millis = localDateToDatePickerMillis(date)

        assertEquals(LocalDate.of(2026, 8, 4), datePickerMillisToLocalDate(millis))
        assertEquals(Instant.parse("2026-08-04T00:00:00Z").toEpochMilli(), millis)
    }
}
