package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY custom_order ASC, name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports ORDER BY custom_order ASC, name COLLATE NOCASE ASC, id ASC")
    suspend fun getAll(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: Long): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Update
    suspend fun update(report: ReportEntity)

    @Query("UPDATE reports SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: String)

    @Query("UPDATE reports SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: String)

    @Query("UPDATE reports SET custom_order = :order WHERE id = :id")
    suspend fun setCustomOrder(id: Long, order: Int)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM reports")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(custom_order), -1) FROM reports")
    suspend fun maxCustomOrder(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE report_id = :reportId ORDER BY date DESC, id DESC")
    suspend fun getByReport(reportId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun exists(id: Long): TransactionEntity?
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name)")
    suspend fun findByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE name = :name AND kind = 'custom'")
    suspend fun deleteCustom(name: String)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingEntity>

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SettingEntity)
}
