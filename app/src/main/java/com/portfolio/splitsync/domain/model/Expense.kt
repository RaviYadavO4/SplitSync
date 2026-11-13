package com.portfolio.splitsync.domain.model

data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val paidByUserId: String,
    val splitType: SplitType,
    val category: String,
    val note: String = "",
    val updatedAt: Long,
    val lastModifiedBy: String,
    val version: Int,
    val syncStatus: SyncStatus,
    val splits: List<ExpenseSplit> = emptyList()
)

data class ExpenseSplit(
    val id: String,
    val expenseId: String,
    val userId: String,
    val amountOwed: Double,
    val paidAmount: Double = 0.0,
    val paymentHistory: String = "",
    val isSettled: Boolean = false
) {
    val totalShare: Double get() = amountOwed + paidAmount
    val installmentsList: List<String> get() = paymentHistory.split("|").filter { it.isNotBlank() }
}
