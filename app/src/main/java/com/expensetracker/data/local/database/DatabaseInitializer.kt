package com.expensetracker.data.local.database

import com.expensetracker.data.local.dao.*
import com.expensetracker.data.local.entity.*
import com.expensetracker.domain.model.DEFAULT_CATEGORIES
import com.expensetracker.util.IdGenerator
import com.expensetracker.util.nowIsoTimestamp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val SETTING_THEME = "themeMode"
private const val SETTING_CURRENT_REPORT = "currentReportId"
private const val SETTING_MIGRATION_HINT = "migrationHintDismissed"
private const val SETTING_REPORT_SORT = "reportSortMode"
private const val SETTING_TRANSACTION_SORT = "transactionSortMode"

@Singleton
class DatabaseInitializer @Inject constructor(
    private val reportDao: ReportDao,
    private val categoryDao: CategoryDao,
    private val settingDao: SettingDao
) {
    private val mutex = Mutex()
    private var initialized = false

    suspend fun ensureInitialized() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            initialize()
            initialized = true
        }
    }

    private suspend fun initialize() {
        // Seed default categories
        DEFAULT_CATEGORIES.forEach { name ->
            categoryDao.insertIgnore(CategoryEntity(name = name, kind = "default"))
        }

        // Create default report if none exists
        if (reportDao.count() == 0) {
            val reportId = IdGenerator.newId()
            reportDao.insert(
                ReportEntity(
                    id = reportId,
                    name = "My Report",
                    updatedAt = nowIsoTimestamp(),
                    customOrder = 0
                )
            )
            settingDao.upsert(SettingEntity(SETTING_CURRENT_REPORT, reportId.toString()))
        }

        // Normalize custom order
        normalizeReportOrder()

        // Seed default settings
        if (settingDao.getValue(SETTING_THEME) == null) {
            settingDao.upsert(SettingEntity(SETTING_THEME, "light"))
        }
        if (settingDao.getValue(SETTING_MIGRATION_HINT) == null) {
            settingDao.upsert(SettingEntity(SETTING_MIGRATION_HINT, "false"))
        }
        if (settingDao.getValue(SETTING_REPORT_SORT) == null) {
            settingDao.upsert(SettingEntity(SETTING_REPORT_SORT, "name"))
        }
        if (settingDao.getValue(SETTING_TRANSACTION_SORT) == null) {
            settingDao.upsert(SettingEntity(SETTING_TRANSACTION_SORT, "recent"))
        }
    }

    suspend fun normalizeReportOrder() {
        val reports = reportDao.getAll()
        val now = nowIsoTimestamp()
        reports.forEachIndexed { index, report ->
            var needsUpdate = false
            var updatedAt = report.updatedAt
            if (report.updatedAt.isBlank()) {
                updatedAt = now
                needsUpdate = true
            }
            if (report.customOrder != index) {
                needsUpdate = true
            }
            if (needsUpdate) {
                reportDao.update(report.copy(updatedAt = updatedAt, customOrder = index))
            }
        }
    }
}
