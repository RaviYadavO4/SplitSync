package com.portfolio.splitsync.data.local.dao

import androidx.room.*
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.data.local.entity.GroupEntity
import com.portfolio.splitsync.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

data class ExpenseWithSplits(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val splits: List<ExpenseSplitEntity>
)

@Dao
interface ExpenseDao {

    // UI ke liye Reactive Flows
    @Query("SELECT * FROM `groups` WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE id = :groupId LIMIT 1")
    fun observeGroupById(groupId: String): Flow<GroupEntity?>

    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeGroupExpenses(groupId: String): Flow<List<ExpenseWithSplits>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_splits")
    fun observeAllSplits(): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT COUNT(*) FROM expenses WHERE syncStatus != 'SYNCED'")
    fun observePendingSyncCount(): Flow<Int>

    // Insert & Safe In-Place Update (Prevents ForeignKey CASCADE deletion)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroupOnly(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpenseOnly(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Transaction
    suspend fun saveExpenseWithSplits(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>) {
        val rowId = insertExpense(expense)
        if (rowId == -1L) {
            updateExpenseOnly(expense)
        }
        insertSplits(splits)
    }

    @Transaction
    suspend fun upsertGroupSafely(group: GroupEntity) {
        val rowId = insertGroup(group)
        if (rowId == -1L) {
            updateGroupOnly(group)
        }
    }

    // Group Members & Soft Delete Queries
    @Query("SELECT * FROM `groups` WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("""
        UPDATE `groups` 
        SET memberIds = :members, 
            memberPhones = :memberPhones, 
            updatedAt = :updatedAt, 
            syncStatus = :status 
        WHERE id = :groupId
    """)
    suspend fun updateGroupMembers(
        groupId: String,
        members: List<String>,
        memberPhones: Map<String, String>,
        updatedAt: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_UPDATE
    )

    @Query("""
        UPDATE expenses 
        SET isDeleted = :isDeleted, 
            updatedAt = :updatedAt, 
            syncStatus = :status 
        WHERE id = :expenseId
    """)
    suspend fun setExpenseDeletedState(
        expenseId: String,
        isDeleted: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_DELETE
    )

    @Query("""
        UPDATE `groups` 
        SET isDeleted = 1, 
            updatedAt = :updatedAt, 
            syncStatus = :status 
        WHERE id = :groupId
    """)
    suspend fun softDeleteGroup(
        groupId: String,
        updatedAt: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_DELETE
    )

    @Query("""
        UPDATE expense_splits 
        SET isSettled = 1 
        WHERE expenseId IN (SELECT id FROM expenses WHERE groupId = :groupId) 
        AND (userId = :memberName OR expenseId IN (SELECT id FROM expenses WHERE groupId = :groupId AND paidByUserId = :memberName))
    """)
    suspend fun settleGroupMemberSplits(groupId: String, memberName: String)

    // WorkManager Sync Queries
    @Query("SELECT * FROM `groups` WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedGroups(): List<GroupEntity>

    @Query("UPDATE `groups` SET syncStatus = :status WHERE id = :groupId")
    suspend fun updateGroupSyncStatus(groupId: String, status: SyncStatus)

    @Query("SELECT * FROM expenses WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Query("UPDATE expenses SET syncStatus = :status, version = :newVersion WHERE id = :id")
    suspend fun updateSyncState(id: String, status: SyncStatus, newVersion: Int)

    // ExpenseDao.kt ke andar add karein:

    @Query("UPDATE expense_splits SET isSettled = 1, amountOwed = 0.0 WHERE id = :splitId")
    suspend fun markSplitFullySettled(splitId: String)

    @Query("UPDATE expense_splits SET amountOwed = :remainingAmount WHERE id = :splitId")
    suspend fun updateSplitRemainingAmount(splitId: String, remainingAmount: Double)

    @Query("""
    UPDATE expense_splits 
    SET amountOwed = :remainingAmount, 
        paidAmount = paidAmount + :justPaidAmount, 
        paymentHistory = CASE 
            WHEN paymentHistory = '' THEN :historyEntry 
            ELSE paymentHistory || '|' || :historyEntry 
        END,
        isSettled = :isFullySettled 
    WHERE id = :splitId
""")
    suspend fun applyPaymentToSplit(
        splitId: String,
        remainingAmount: Double,
        justPaidAmount: Double,
        historyEntry: String,
        isFullySettled: Boolean
    )





}
