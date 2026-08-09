package com.example.kakeibo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, CategoryBudgetEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kakeibo.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS budget_settings (id INTEGER NOT NULL, amount INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(id))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS category_budgets (category_id INTEGER NOT NULL, amount INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(category_id), FOREIGN KEY(category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_category_budgets_category_id ON category_budgets(category_id)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS monthly_budgets")
                database.execSQL("DROP TABLE IF EXISTS budget_settings")
            }
        }
    }
}

fun defaultCategories(): List<CategoryEntity> = listOf(
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "食費", iconKey = "restaurant", colorArgb = 0xFFE57373.toInt(), displayOrder = 0, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "日用品", iconKey = "shopping", colorArgb = 0xFF64B5F6.toInt(), displayOrder = 1, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "住居費", iconKey = "home", colorArgb = 0xFF9575CD.toInt(), displayOrder = 2, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "光熱費", iconKey = "bolt", colorArgb = 0xFFFFB74D.toInt(), displayOrder = 3, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "通信費", iconKey = "phone", colorArgb = 0xFF4DB6AC.toInt(), displayOrder = 4, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "交通費", iconKey = "train", colorArgb = 0xFF81C784.toInt(), displayOrder = 5, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "医療費", iconKey = "medical", colorArgb = 0xFFF06292.toInt(), displayOrder = 6, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "娯楽", iconKey = "movie", colorArgb = 0xFF7986CB.toInt(), displayOrder = 7, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.EXPENSE, name = "その他", iconKey = "more", colorArgb = 0xFF90A4AE.toInt(), displayOrder = 8, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.INCOME, name = "給与", iconKey = "work", colorArgb = 0xFF66BB6A.toInt(), displayOrder = 0, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.INCOME, name = "賞与", iconKey = "celebration", colorArgb = 0xFF26A69A.toInt(), displayOrder = 1, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.INCOME, name = "副業", iconKey = "laptop", colorArgb = 0xFF42A5F5.toInt(), displayOrder = 2, isDefault = true),
    CategoryEntity(type = com.example.kakeibo.model.TransactionType.INCOME, name = "その他", iconKey = "more", colorArgb = 0xFF90A4AE.toInt(), displayOrder = 3, isDefault = true)
)
