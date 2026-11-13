package com.portfolio.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId"), Index("userId")]
)
data class ExpenseSplitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val expenseId: String,
    val userId: String,
    val amountOwed: Double,
    val paidAmount: Double = 0.0,
    val paymentHistory: String = "", // e.g. "₹30.00 (12 Nov)|₹40.00 (13 Nov)"
    val isSettled: Boolean = false
)
