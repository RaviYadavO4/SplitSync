package com.portfolio.splitsync.ui.screens.group

import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits

data class MemberBalance(
    val memberName: String,
    val phone: String = "",
    val netAmount: Double
)

data class GroupDetailUiState(
    val groupId: String = "",
    val groupName: String = "Group",
    val inviteCode: String = "",
    val currentUserName: String = "You",
    val members: List<String> = emptyList(),
    val expenses: List<ExpenseWithSplits> = emptyList(),
    val totalGroupSpend: Double = 0.0,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val memberBalances: List<MemberBalance> = emptyList(),
    val recentActivityMessage: String? = null
)
