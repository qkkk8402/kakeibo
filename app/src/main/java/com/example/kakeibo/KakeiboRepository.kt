package com.example.kakeibo

import com.example.kakeibo.data.AppDatabase
import com.example.kakeibo.data.CategoryEntity
import com.example.kakeibo.data.CategoryBudgetEntity
import com.example.kakeibo.data.TransactionEntity
import com.example.kakeibo.model.CategoryModel
import com.example.kakeibo.model.MAX_AMOUNT_YEN
import com.example.kakeibo.model.normalizeMemo
import com.example.kakeibo.model.validateCategoryName
import com.example.kakeibo.model.MonthlySummary
import com.example.kakeibo.model.TransactionModel
import com.example.kakeibo.model.TransactionType
import com.example.kakeibo.model.CategoryBudgetSummary
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
                    memo = row.transaction.memo,
                    iconKey = row.categoryIconKey,
                    colorArgb = row.categoryColorArgb
                )
            }
        }
    }

    fun observeSummary(month: YearMonth): Flow<MonthlySummary> {
        val range = monthRange(month)
        return combine(
            db.transactionDao().observeTotals(range.first, range.last + 1),
            db.categoryDao().observeActive(),
            db.budgetDao().observeCategoryBudgets()
        ) { totals, categories, budgets ->
            val activeExpenseIds = categories
                .filter { it.type == TransactionType.EXPENSE }
                .mapTo(mutableSetOf()) { it.id }
            val totalBudget = budgets
                .filter { it.categoryId in activeExpenseIds }
                .sumOf { it.amount }
                .takeIf { it > 0 }
            MonthlySummary(month, totals.income, totals.expense, totalBudget)
        }
    }

    fun observeCategoryBudgetSummaries(month: YearMonth): Flow<List<CategoryBudgetSummary>> {
        val range = monthRange(month)
        return combine(
            db.categoryDao().observeActive(),
            db.budgetDao().observeCategoryBudgets(),
            db.transactionDao().observeExpenseByCategory(range.first, range.last + 1)
        ) { categories, budgets, spent ->
            val budgetByCategory = budgets.associateBy { it.categoryId }
            val spentByCategory = spent.associate { it.categoryId to it.spent }
            categories
                .filter { it.type == TransactionType.EXPENSE }
                .map { category ->
                    CategoryBudgetSummary(
                        category = category.toModel(),
                        spent = spentByCategory[category.id] ?: 0,
                        budget = budgetByCategory[category.id]?.amount
                    )
                }
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
                row.transaction.memo,
                row.categoryIconKey,
                row.categoryColorArgb
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

    suspend fun saveCategoryBudget(categoryId: Long, amount: Long?) {
        if (amount == null) db.budgetDao().deleteCategoryBudget(categoryId)
        else {
            require(amount in 1..MAX_AMOUNT_YEN) { "予算は1円から${MAX_AMOUNT_YEN}円までで入力してください" }
            db.budgetDao().upsertCategoryBudget(CategoryBudgetEntity(categoryId = categoryId, amount = amount))
        }
    }

    suspend fun addCategory(type: TransactionType, name: String, iconKey: String = "more", colorArgb: Int = 0xFF90A4AE.toInt()) {
        val cleaned = name.trim()
        require(validateCategoryName(cleaned) == null) { validateCategoryName(cleaned)!! }
        db.categoryDao().insert(
            CategoryEntity(type = type, name = cleaned, iconKey = iconKey, colorArgb = colorArgb, displayOrder = 99)
        )
    }

    suspend fun setCategoryActive(id: Long, active: Boolean) = db.categoryDao().setActive(id, active)

    suspend fun renameCategory(id: Long, name: String) {
        val cleaned = name.trim()
        require(validateCategoryName(cleaned) == null) { validateCategoryName(cleaned)!! }
        db.categoryDao().rename(id, cleaned)
    }

    suspend fun updateCategoryAppearance(id: Long, iconKey: String, colorArgb: Int) =
        db.categoryDao().updateAppearance(id, iconKey, colorArgb)
}

private fun CategoryEntity.toModel() = CategoryModel(id, type, name, iconKey, colorArgb, displayOrder, isActive)
