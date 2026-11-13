package com.portfolio.splitsync.domain.model

data class Group(
    val id: String,
    val name: String,
    val description: String,
    val inviteCode: String,
    val currencySymbol: String = "₹",
    val memberIds: List<String>,
    val memberPhones: Map<String, String> = emptyMap(),
    val createdBy: String,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)
