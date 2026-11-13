package com.portfolio.splitsync.data.mapper

import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.data.local.entity.GroupEntity
import com.portfolio.splitsync.domain.model.Expense
import com.portfolio.splitsync.domain.model.ExpenseSplit
import com.portfolio.splitsync.domain.model.Group

fun GroupEntity.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    inviteCode = inviteCode,
    currencySymbol = currencySymbol,
    memberIds = memberIds,
    memberPhones = memberPhones,
    createdBy = createdBy,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun Group.toEntity(): GroupEntity = GroupEntity(
    id = id,
    name = name,
    description = description,
    inviteCode = inviteCode,
    currencySymbol = currencySymbol,
    memberIds = memberIds,
    memberPhones = memberPhones,
    createdBy = createdBy,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

// Expense & Split Mappers
fun ExpenseSplitEntity.toDomain(): ExpenseSplit = ExpenseSplit(
    id = id,
    expenseId = expenseId,
    userId = userId,
    amountOwed = amountOwed,
    paidAmount = paidAmount,
    paymentHistory = paymentHistory,
    isSettled = isSettled
)

fun ExpenseSplit.toEntity(): ExpenseSplitEntity = ExpenseSplitEntity(
    id = id,
    expenseId = expenseId,
    userId = userId,
    amountOwed = amountOwed,
    paidAmount = paidAmount,
    paymentHistory = paymentHistory,
    isSettled = isSettled
)

fun ExpenseWithSplits.toDomain(): Expense = Expense(
    id = expense.id,
    groupId = expense.groupId,
    title = expense.title,
    amount = expense.amount,
    paidByUserId = expense.paidByUserId,
    splitType = expense.splitType,
    category = expense.category,
    note = expense.note,
    updatedAt = expense.updatedAt,
    lastModifiedBy = expense.lastModifiedBy,
    version = expense.version,
    syncStatus = expense.syncStatus,
    splits = splits.map { it.toDomain() }
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    groupId = groupId,
    title = title,
    amount = amount,
    paidByUserId = paidByUserId,
    splitType = splitType,
    category = category,
    note = note,
    updatedAt = updatedAt,
    lastModifiedBy = lastModifiedBy,
    version = version,
    syncStatus = syncStatus
)
