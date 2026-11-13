package com.portfolio.splitsync.domain.repository

import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeGroups(): Flow<List<GroupEntity>>
    fun observeGroupById(groupId: String): Flow<GroupEntity?>
    fun observeGroupExpenses(groupId: String): Flow<List<ExpenseWithSplits>>
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>>
    fun observePendingSyncCount(): Flow<Int>

    suspend fun createGroup(group: GroupEntity)
    suspend fun addMemberToGroup(groupId: String, memberName: String, rawPhone: String = "")
    suspend fun syncGroupsForUserPhone(phone: String, userName: String)
    suspend fun joinGroupByInviteCode(inviteCode: String): Result<String>
    fun observeRealtimeGroupSync(groupId: String): Flow<Unit>

    suspend fun addOrUpdateExpense(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        isEdit: Boolean = false
    )
    suspend fun deleteExpense(expenseId: String)
    suspend fun restoreDeletedExpense(expenseId: String)
    suspend fun deleteGroup(groupId: String)

    suspend fun recordFifoSettlement(
        groupId: String,
        memberName: String,
        settledAmount: Double,
        isMemberPayingMe: Boolean,
        groupExpenses: List<ExpenseWithSplits>
    )

    suspend fun resolveExpenseConflict(expense: ExpenseEntity, keepLocal: Boolean)
    fun triggerBackgroundSync()
}
