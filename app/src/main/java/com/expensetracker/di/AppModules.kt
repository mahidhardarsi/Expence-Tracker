package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.local.dao.*
import com.expensetracker.data.local.database.AppDatabase
import com.expensetracker.data.repository.ExpenseRepositoryImpl
import com.expensetracker.domain.repository.AiFinanceRepository
import com.expensetracker.domain.repository.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideSettingDao(db: AppDatabase): SettingDao = db.settingDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindAiFinanceRepository(impl: ExpenseRepositoryImpl): AiFinanceRepository
}
