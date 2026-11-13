package com.portfolio.splitsync.domain.usecase

import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import javax.inject.Inject

data class GlobalBalanceSummary(
    val youAreOwed: Double,
    val youOwe: Double
) {
    val netBalance: Double get() = youAreOwed - youOwe
}

class CalculateNetBalanceUseCase @Inject constructor() {


    fun calculateGlobalSummary(
        currentUser: String,
        expenses: List<ExpenseEntity>,
        splits: List<ExpenseSplitEntity>
    ): GlobalBalanceSummary {
        var owedToMe = 0.0
        var iOwe = 0.0
        val activeExpensesMap = expenses
            .filter { !it.isDeleted && it.category != "Settlement" }
            .associateBy { it.id }

        for (split in splits) {
            if (split.isSettled || split.amountOwed <= 0.0) continue
            val parentExpense = activeExpensesMap[split.expenseId] ?: continue

            if (parentExpense.paidByUserId == currentUser && split.userId != currentUser) {
                owedToMe += split.amountOwed
            } else if (parentExpense.paidByUserId != currentUser && split.userId == currentUser) {
                iOwe += split.amountOwed
            }
        }

        return GlobalBalanceSummary(
            youAreOwed = owedToMe,
            youOwe = iOwe
        )
    }


    fun calculateGroupMemberBalances(
        currentUser: String,
        groupExpenses: List<ExpenseWithSplits>
    ): Map<String, Double> {
        val balanceMap = mutableMapOf<String, Double>()

        for (item in groupExpenses) {
            val exp = item.expense
            if (exp.isDeleted || exp.category == "Settlement") continue

            for (split in item.splits) {
                if (split.isSettled || split.amountOwed <= 0.0) continue

                if (exp.paidByUserId == currentUser && split.userId != currentUser) {
                    balanceMap[split.userId] = (balanceMap[split.userId] ?: 0.0) + split.amountOwed
                } else if (exp.paidByUserId != currentUser && split.userId == currentUser) {
                    balanceMap[exp.paidByUserId] = (balanceMap[exp.paidByUserId] ?: 0.0) - split.amountOwed
                }
            }
        }

        return balanceMap
    }
}
