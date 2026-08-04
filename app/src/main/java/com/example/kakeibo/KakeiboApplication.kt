package com.example.kakeibo

import android.app.Application
import com.example.kakeibo.data.AppDatabase
import com.example.kakeibo.data.defaultCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KakeiboApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { KakeiboRepository(database) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (database.categoryDao().count() == 0) {
                database.categoryDao().insertAll(defaultCategories())
            }
        }
    }
}
