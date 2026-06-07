package com.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: Long,
    val name: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "custom_order") val customOrder: Int
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["report_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["report_id"])]
)
data class TransactionEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "report_id") val reportId: Long,
    val name: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: String  // "credit" | "debit"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val kind: String  // "default" | "custom"
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
