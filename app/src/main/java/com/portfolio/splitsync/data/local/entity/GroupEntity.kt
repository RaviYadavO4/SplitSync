package com.portfolio.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.portfolio.splitsync.domain.model.SyncStatus
import java.util.UUID

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val inviteCode: String = generateInviteCode(),
    val currencySymbol: String = "₹",
    val memberIds: List<String>,
    val memberPhones: Map<String, String> = emptyMap(), // Member Name -> 10-digit Phone
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val isDeleted: Boolean = false
)

fun generateInviteCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}
