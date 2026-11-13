package com.portfolio.splitsync.ui.screens.home

import com.portfolio.splitsync.data.local.entity.GroupEntity

data class HomeUiState(
    val currentUserName: String = "You",
    val currentUserPhone: String = "",
    val isOnline: Boolean = true,
    val pendingSyncCount: Int = 0,
    val groups: List<GroupEntity> = emptyList(),
    val youAreOwed: Double = 0.0,
    val youOwe: Double = 0.0,
    val joinMessage: String? = null
)
