package com.example.kakeibo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MonthlyTotals(
    val income: Long,
    val expense: Long
)

data class CategoryMonthlyTotal(
    @androidx.room.ColumnInfo(name = "category_id") val categoryId: Long,
    val spent: Long
)

data class TransactionWithCategory(
    @androidx.room.Embedded val transaction: TransactionEntity,
    @androidx.room.ColumnInfo(name = "category_name") val categoryName: String,
    @androidx.room.ColumnInfo(name = "category_icon_key") val categoryIconKey: String,
    @androidx.room.ColumnInfo(name = "category_color_argb") val categoryColorArgb: Int
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY type, display_order, id")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type, display_order, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET is_active = :active, updated_at = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE categories SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE categories SET icon_key = :iconKey, color_argb = :colorArgb, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateAppearance(id: Long, iconKey: String, colorArgb: Int, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon_key AS category_icon_key, c.color_argb AS category_color_argb
        FROM transactions t INNER JOIN categories c ON c.id = t.category_id
        WHERE t.transaction_date >= :fromDay AND t.transaction_date < :untilDay
        ORDER BY t.transaction_date DESC, t.created_at DESC
        """
    )
    fun observeInRange(fromDay: Long, untilDay: Long): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.icon_key AS category_icon_key, c.color_argb AS category_color_argb
        FROM transactions t INNER JOIN categories c ON c.id = t.category_id
        WHERE t.id = :id
        """
    )
    suspend fun findWithCategory(id: Long): TransactionWithCategory?

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
          COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
        FROM transactions
        WHERE transaction_date >= :fromDay AND transaction_date < :untilDay
        """
    )
    fun observeTotals(fromDay: Long, untilDay: Long): Flow<MonthlyTotals>

    @Query(
        """
        SELECT category_id, COALESCE(SUM(amount), 0) AS spent
        FROM transactions
        WHERE transaction_date >= :fromDay AND transaction_date < :untilDay AND type = 'EXPENSE'
        GROUP BY category_id
        """
    )
    fun observeExpenseByCategory(fromDay: Long, untilDay: Long): Flow<List<CategoryMonthlyTotal>>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM category_budgets ORDER BY category_id")
    fun observeCategoryBudgets(): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE category_id = :categoryId")
    suspend fun deleteCategoryBudget(categoryId: Long)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllCategoryBudgets()
}
