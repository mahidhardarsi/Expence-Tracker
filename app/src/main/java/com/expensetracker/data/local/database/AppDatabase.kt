package com.expensetracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.data.local.dao.*
import com.expensetracker.data.local.entity.*

@Database(
    entities = [
        ReportEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun settingDao(): SettingDao

    companion object {
        const val DATABASE_NAME = "expense-tracker.db"
    }
}
