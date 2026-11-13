package com.portfolio.splitsync.domain.usecase

import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.domain.model.SplitType
import javax.inject.Inject
import kotlin.math.roundToInt

class CalculateSplitUseCase @Inject constructor() {

    operator fun invoke(
        expenseId: String,
        totalAmount: Double,
        members: List<String>,
        paidBy: String,
        splitType: SplitType,
        customSplits: Map<String, Double> = emptyMap()
    ): List<ExpenseSplitEntity> {
        if (members.isEmpty() || totalAmount <= 0.0) return emptyList()

        return when (splitType) {
            SplitType.EQUAL -> {
                val rawShare = totalAmount / members.size
                val roundedShare = (rawShare * 100.0).roundToInt() / 100.0

                members.map { memberName ->
                    ExpenseSplitEntity(
                        expenseId = expenseId,
                        userId = memberName,
                        amountOwed = roundedShare,
                        paidAmount = 0.0,
                        paymentHistory = "",
                        isSettled = (memberName == paidBy) || (roundedShare <= 0.0)
                    )
                }
            }

            SplitType.EXACT -> {
                members.map { memberName ->
                    val exactShare = ((customSplits[memberName] ?: 0.0) * 100.0).roundToInt() / 100.0
                    ExpenseSplitEntity(
                        expenseId = expenseId,
                        userId = memberName,
                        amountOwed = exactShare,
                        paidAmount = 0.0,
                        paymentHistory = "",
                        isSettled = (memberName == paidBy) || (exactShare <= 0.0)
                    )
                }
            }

            SplitType.PERCENTAGE -> {
                members.map { memberName ->
                    val pct = customSplits[memberName] ?: 0.0
                    val share = ((totalAmount * (pct / 100.0)) * 100.0).roundToInt() / 100.0
                    ExpenseSplitEntity(
                        expenseId = expenseId,
                        userId = memberName,
                        amountOwed = share,
                        paidAmount = 0.0,
                        paymentHistory = "",
                        isSettled = (memberName == paidBy) || (share <= 0.0)
                    )
                }
            }
        }
    }
}
