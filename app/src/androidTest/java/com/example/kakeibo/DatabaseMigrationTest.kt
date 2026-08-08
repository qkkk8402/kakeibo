package com.example.kakeibo

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kakeibo.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2PreservesExistingDataAndCreatesBudgetTables() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL(
                "INSERT INTO monthly_budgets(year_month, amount, created_at, updated_at) " +
                    "VALUES ('2026-08', 100000, 1, 1)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("migration-test", 2, true, AppDatabase.MIGRATION_1_2)
        db.query("SELECT amount FROM monthly_budgets WHERE year_month = '2026-08'").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(100000L, cursor.getLong(0))
        }
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name IN ('budget_settings', 'category_budgets') ORDER BY name"
        ).use { cursor ->
            var count = 0
            while (cursor.moveToNext()) count++
            assertEquals(2, count)
        }
        db.close()
    }
}
