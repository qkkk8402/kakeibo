package com.example.kakeibo

import com.example.kakeibo.data.AppDatabase
import com.example.kakeibo.data.CategoryEntity
import com.example.kakeibo.data.MonthlyBudgetEntity
import com.example.kakeibo.data.TransactionEntity
import com.example.kakeibo.model.CategoryModel
import com.example.kakeibo.model.MAX_AMOUNT_YEN
import com.example.kakeibo.model.normalizeMemo
import com.example.kakeibo.model.validateCategoryName
import com.example.kakeibo.model.MonthlySummary
import com.example.kakeibo.model.TransactionModel
import com.example.kakeibo.model.TransactionType
import com.example.kakeibo.model.monthRange
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class KakeiboRepository(private val db: AppDatabase) {
    fun observeCategories(activeOnly: Boolean = true): Flow<List<CategoryModel>> =
        (if (activeOnly) db.categoryDao().observeActive() else db.categoryDao().observeAll())
            .map { list -> list.map(CategoryEntity::toModel) }

    fun observeTransactions(month: YearMonth): Flow<List<TransactionModel>> {
        val range = monthRange(month)
        return db.transactionDao().observeInRange(range.first, range.last + 1).map { list ->
            list.map { row ->
                TransactionModel(
                    id = row.transaction.id,
                    type = row.transaction.type,
                    amount = row.transaction.amount,
                    categoryId = row.transaction.categoryId,
                    categoryName = row.categoryName,
                    date = LocalDate.ofEpochDay(row.transaction.dateEpochDay),
                    memo = row.transaction.memo
                )
            }
        }
    }

    fun observeSummary(month: YearMonth): Flow<MonthlySummary> {
        val range = monthRange(month)
        return combine(
            db.transactionDao().observeTotals(range.first, range.last + 1),
            db.budgetDao().observe(month.toString())
        ) { totals, budget ->
            MonthlySummary(month, totals.income, totals.expense, budget?.amount)
        }
    }

    suspend fun findTransaction(id: Long): TransactionModel? =
        db.transactionDao().findWithCategory(id)?.let { row ->
            TransactionModel(
                row.transaction.id,
                row.transaction.type,
                row.transaction.amount,
                row.transaction.categoryId,
                row.categoryName,
                LocalDate.ofEpochDay(row.transaction.dateEpochDay),
                row.transaction.memo
            )
        }

    suspend fun saveTransaction(
        id: Long?,
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        date: LocalDate,
        memo: String?
    ) {
        require(amount in 1..MAX_AMOUNT_YEN) { "金額は1円から${MAX_AMOUNT_YEN}円までで入力してください" }
        val cleanedMemo = normalizeMemo(memo)
        if (id == null) {
            db.transactionDao().insert(
                TransactionEntity(type = type, amount = amount, categoryId = categoryId, dateEpochDay = date.toEpochDay(), memo = cleanedMemo)
            )
        } else {
            val existing = db.transactionDao().findWithCategory(id)?.transaction
                ?: error("収支記録が見つかりません")
            db.transactionDao().update(
                existing.copy(type = type, amount = amount, categoryId = categoryId, dateEpochDay = date.toEpochDay(), memo = cleanedMemo, updatedAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun deleteTransaction(id: Long) {
        db.transactionDao().findWithCategory(id)?.let { db.transactionDao().delete(it.transaction) }
    }

    suspend fun saveBudget(month: YearMonth, amount: Long?) {
        if (amount == null) db.budgetDao().delete(month.toString())
        else {
            require(amount in 1..MAX_AMOUNT_YEN) { "予算は1円から${MAX_AMOUNT_YEN}円までで入力してください" }
            db.budgetDao().upsert(MonthlyBudgetEntity(month.toString(), amount))
        }
    }

    suspend fun addCategory(type: TransactionType, name: String) {
        val cleaned = name.trim()
        require(validateCategoryName(cleaned) == null) { validateCategoryName(cleaned)!! }
        db.categoryDao().insert(
            CategoryEntity(type = type, name = cleaned, iconKey = "more", colorArgb = 0xFF90A4AE.toInt(), displayOrder = 99)
        )
    }

    suspend fun setCategoryActive(id: Long, active: Boolean) = db.categoryDao().setActive(id, active)

    suspend fun renameCategory(id: Long, name: String) {
        val cleaned = name.trim()
        require(validateCategoryName(cleaned) == null) { validateCategoryName(cleaned)!! }
        db.categoryDao().rename(id, cleaned)
    }
}

private fun CategoryEntity.toModel() = CategoryModel(id, type, name, iconKey, colorArgb, displayOrder, isActive)
