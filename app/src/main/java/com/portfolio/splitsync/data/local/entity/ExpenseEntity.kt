package com.portfolio.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.portfolio.splitsync.domain.model.SplitType
import com.portfolio.splitsync.domain.model.SyncStatus
import java.util.UUID

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId"), Index("syncStatus")]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val title: String,
    val amount: Double,
    val paidByUserId: String,
    val splitType: SplitType = SplitType.EQUAL,
    val category: String = "General",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val lastModifiedBy: String,
    val version: Int = 1, // Conflict detection ke liye
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val isDeleted: Boolean = false
)
