package com.example.kakeibo.model

import java.time.LocalDate
import java.time.YearMonth

enum class TransactionType { INCOME, EXPENSE }

data class MonthlySummary(
    val month: YearMonth,
    val income: Long = 0,
    val expense: Long = 0,
    val budget: Long? = null
) {
    val balance: Long get() = income - expense
    val remainingBudget: Long? get() = budget?.minus(expense)
    val usageRatio: Float? get() = budget?.takeIf { it > 0 }?.let { expense.toFloat() / it }
}

data class CategoryModel(
    val id: Long,
    val type: TransactionType,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val displayOrder: Int,
    val isActive: Boolean
)

data class TransactionModel(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val categoryId: Long,
    val categoryName: String,
    val date: LocalDate,
    val memo: String?
)

fun monthRange(month: YearMonth): LongRange {
    return month.atDay(1).toEpochDay() until month.plusMonths(1).atDay(1).toEpochDay()
}
