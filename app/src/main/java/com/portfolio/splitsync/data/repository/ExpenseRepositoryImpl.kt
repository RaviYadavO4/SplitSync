package com.portfolio.splitsync.data.repository

import android.content.Context
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.portfolio.splitsync.data.local.dao.ExpenseDao
import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.data.local.entity.GroupEntity
import com.portfolio.splitsync.data.sync.ExpenseSyncWorker
import com.portfolio.splitsync.domain.model.SyncStatus
import com.portfolio.splitsync.domain.repository.ExpenseRepository
import com.portfolio.splitsync.domain.usecase.ConflictResolutionResult
import com.portfolio.splitsync.domain.usecase.ResolveConflictUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseDao: ExpenseDao,
    private val firestore: FirebaseFirestore,
    private val resolveConflictUseCase: ResolveConflictUseCase,
    private val authRepository: AuthRepositoryImpl
): ExpenseRepository {
    override fun observeGroups(): Flow<List<GroupEntity>> = expenseDao.observeAllGroups()

    override fun observeGroupById(groupId: String): Flow<GroupEntity?> = expenseDao.observeGroupById(groupId)

    override fun observeGroupExpenses(groupId: String): Flow<List<ExpenseWithSplits>> =
        expenseDao.observeGroupExpenses(groupId)

    override fun observeAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAllExpenses()

    override fun observeAllSplits(): Flow<List<ExpenseSplitEntity>> = expenseDao.observeAllSplits()

    override fun observePendingSyncCount(): Flow<Int> = expenseDao.observePendingSyncCount()

    override suspend fun createGroup(group: GroupEntity) {
        expenseDao.upsertGroupSafely(group.copy(syncStatus = SyncStatus.PENDING_CREATE))
        triggerBackgroundSync()
    }

    override suspend fun addMemberToGroup(groupId: String, memberName: String, rawPhone: String) {
        val cleanName = memberName.trim()
        if (cleanName.isBlank()) return

        val group = expenseDao.getGroupById(groupId) ?: return
        if (group.memberIds.any { it.equals(cleanName, ignoreCase = true) }) return

        val normalizedPhone = authRepository.normalizePhone(rawPhone)
        val updatedMembers = group.memberIds + cleanName
        val updatedPhones = group.memberPhones.toMutableMap().apply {
            if (normalizedPhone.length == 10) {
                put(cleanName, normalizedPhone)
            }
        }

        expenseDao.updateGroupMembers(
            groupId = groupId,
            members = updatedMembers,
            memberPhones = updatedPhones
        )
        triggerBackgroundSync()
    }

    override suspend fun syncGroupsForUserPhone(phone: String, userName: String) {
        try {
            val snapshot = firestore.collection("groups")
                .whereArrayContains("phoneList", phone)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val groupId = doc.id
                val name = doc.getString("name") ?: "Shared Group"
                val description = doc.getString("description") ?: ""
                val inviteCode = doc.getString("inviteCode") ?: ""
                val createdBy = doc.getString("createdBy") ?: ""
                val isDeleted = doc.getBoolean("isDeleted") ?: false
                val members = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: listOf(userName)
                @Suppress("UNCHECKED_CAST")
                val phonesMap = (doc.get("memberPhones") as? Map<String, String>) ?: emptyMap()

                val localGroup = GroupEntity(
                    id = groupId,
                    name = name,
                    description = description,
                    inviteCode = inviteCode,
                    memberIds = members,
                    memberPhones = phonesMap,
                    createdBy = createdBy,
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = isDeleted
                )
                expenseDao.upsertGroupSafely(localGroup)
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun joinGroupByInviteCode(inviteCode: String): Result<String> {
        return try {
            val userName = authRepository.currentUserName
            val userPhone = authRepository.currentUserPhone
            val cleanCode = inviteCode.trim().uppercase()

            val querySnapshot = firestore.collection("groups")
                .whereEqualTo("inviteCode", cleanCode)
                .get()
                .await()

            val doc = querySnapshot.documents.firstOrNull()
                ?: return Result.failure(Exception("Invalid code: '$cleanCode' not found"))

            val groupId = doc.id
            val name = doc.getString("name") ?: "Shared Group"
            val description = doc.getString("description") ?: ""
            val createdBy = doc.getString("createdBy") ?: ""
            val existingMembers = (doc.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()

            @Suppress("UNCHECKED_CAST")
            val existingPhones = ((doc.get("memberPhones") as? Map<String, String>) ?: emptyMap()).toMutableMap()

            if (existingMembers.none { it.equals(userName, ignoreCase = true) }) {
                existingMembers.add(userName)
                if (userPhone.length == 10) {
                    existingPhones[userName] = userPhone
                }
                doc.reference.update(
                    mapOf(
                        "memberIds" to existingMembers,
                        "memberPhones" to existingPhones,
                        "phoneList" to existingPhones.values.filter { it.length == 10 }.distinct()
                    )
                ).await()
            }

            val localGroup = GroupEntity(
                id = groupId,
                name = name,
                description = description,
                inviteCode = cleanCode,
                memberIds = existingMembers,
                memberPhones = existingPhones,
                createdBy = createdBy,
                syncStatus = SyncStatus.SYNCED
            )
            expenseDao.upsertGroupSafely(localGroup)
            Result.success(name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRealtimeGroupSync(groupId: String): Flow<Unit> = callbackFlow {
        val groupDocRef = firestore.collection("groups").document(groupId)

        val groupListener = groupDocRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val remoteMembers = (snapshot.get("memberIds") as? List<*>)
                    ?.filterIsInstance<String>() ?: return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val remotePhones = (snapshot.get("memberPhones") as? Map<String, String>) ?: emptyMap()
                val remoteDeleted = snapshot.getBoolean("isDeleted") ?: false

                CoroutineScope(Dispatchers.IO).launch {
                    val localGroup = expenseDao.getGroupById(groupId)
                    if (localGroup != null && localGroup.syncStatus == SyncStatus.SYNCED) {
                        expenseDao.updateGroupOnly(
                            localGroup.copy(
                                memberIds = remoteMembers,
                                memberPhones = remotePhones,
                                isDeleted = remoteDeleted
                            )
                        )
                    }
                }
            }
        }

        val expenseListener = groupDocRef.collection("expenses")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                CoroutineScope(Dispatchers.IO).launch {
                    for (doc in snapshots.documents) {
                        val expId = doc.id
                        val title = doc.getString("title") ?: continue
                        val amount = doc.getDouble("amount") ?: 0.0
                        val paidBy = doc.getString("paidByUserId") ?: "Unknown"
                        val category = doc.getString("category") ?: "General"
                        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        val version = doc.getLong("version")?.toInt() ?: 1
                        val lastModifiedBy = doc.getString("lastModifiedBy") ?: paidBy
                        val isDeleted = doc.getBoolean("isDeleted") ?: false

                        val remoteExpense = ExpenseEntity(
                            id = expId,
                            groupId = groupId,
                            title = title,
                            amount = amount,
                            paidByUserId = paidBy,
                            category = category,
                            updatedAt = updatedAt,
                            lastModifiedBy = lastModifiedBy,
                            version = version,
                            syncStatus = SyncStatus.SYNCED,
                            isDeleted = isDeleted
                        )

                        val localExpense = expenseDao.getExpenseById(expId)
                        if (localExpense == null) {
                            val group = expenseDao.getGroupById(groupId)
                            val members = group?.memberIds ?: listOf(paidBy)
                            val share = if (members.isNotEmpty()) amount / members.size else amount
                            val splits = members.map { member ->
                                ExpenseSplitEntity(
                                    expenseId = expId,
                                    userId = member,
                                    amountOwed = share,
                                    isSettled = member == paidBy
                                )
                            }
                            expenseDao.saveExpenseWithSplits(remoteExpense, splits)
                        } else {
                            when (val res = resolveConflictUseCase.resolve(localExpense, remoteExpense)) {
                                is ConflictResolutionResult.AcceptRemote -> expenseDao.updateExpenseOnly(res.remoteExpense)
                                is ConflictResolutionResult.NeedsManualReview -> expenseDao.updateExpenseOnly(res.local)
                                else -> Unit
                            }
                        }
                    }
                }
            }
        awaitClose {
            groupListener.remove()
            expenseListener.remove()
        }
    }
    override suspend fun addOrUpdateExpense(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        isEdit: Boolean
    ) {
        val updatedExpense = expense.copy(
            updatedAt = System.currentTimeMillis(),
            version = if (isEdit) expense.version + 1 else 1,
            syncStatus = if (isEdit) SyncStatus.PENDING_UPDATE else SyncStatus.PENDING_CREATE
        )
        expenseDao.saveExpenseWithSplits(updatedExpense, splits)
        triggerBackgroundSync()
    }

    override suspend fun deleteExpense(expenseId: String) {
        val existing = expenseDao.getExpenseById(expenseId) ?: return
        expenseDao.setExpenseDeletedState(
            expenseId = expenseId,
            isDeleted = true,
            status = SyncStatus.PENDING_DELETE
        )
        try {
            firestore.collection("groups")
                .document(existing.groupId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis(),
                        "version" to (existing.version + 1)
                    )
                )
                .await()
            expenseDao.updateSyncState(expenseId, SyncStatus.SYNCED, existing.version + 1)
        } catch (_: Exception) {
            triggerBackgroundSync()
        }
    }

    override suspend fun restoreDeletedExpense(expenseId: String) {
        val existing = expenseDao.getExpenseById(expenseId) ?: return
        expenseDao.setExpenseDeletedState(
            expenseId = expenseId,
            isDeleted = false,
            status = SyncStatus.PENDING_UPDATE
        )
        try {
            firestore.collection("groups")
                .document(existing.groupId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "isDeleted" to false,
                        "updatedAt" to System.currentTimeMillis(),
                        "version" to (existing.version + 1)
                    )
                )
                .await()
            expenseDao.updateSyncState(expenseId, SyncStatus.SYNCED, existing.version + 1)
        } catch (_: Exception) {
            triggerBackgroundSync()
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        expenseDao.softDeleteGroup(groupId = groupId)
        try {
            firestore.collection("groups")
                .document(groupId)
                .update(
                    mapOf(
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            expenseDao.updateGroupSyncStatus(groupId, SyncStatus.SYNCED)
        } catch (_: Exception) {
            triggerBackgroundSync()
        }
    }

    suspend fun settleMemberBalance(groupId: String, memberName: String) {
        expenseDao.settleGroupMemberSplits(groupId, memberName)
        triggerBackgroundSync()
    }
    override suspend fun resolveExpenseConflict(expense: ExpenseEntity, keepLocal: Boolean) {
        if (keepLocal) {
            val resolved = expense.copy(
                version = expense.version + 1,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            expenseDao.updateExpenseOnly(resolved)
            triggerBackgroundSync()
        } else {
            try {
                val snapshot = firestore.collection("groups")
                    .document(expense.groupId)
                    .collection("expenses")
                    .document(expense.id)
                    .get()
                    .await()
                if (snapshot.exists()) {
                    val cloudExpense = expense.copy(
                        title = snapshot.getString("title") ?: expense.title,
                        amount = snapshot.getDouble("amount") ?: expense.amount,
                        version = snapshot.getLong("version")?.toInt() ?: expense.version,
                        updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis(),
                        syncStatus = SyncStatus.SYNCED
                    )
                    expenseDao.updateExpenseOnly(cloudExpense)
                }
            } catch (_: Exception) {
            }
        }
    }
    override fun triggerBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<ExpenseSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "splitsync_immediate_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }


    override suspend fun recordFifoSettlement(
        groupId: String,
        memberName: String,
        settledAmount: Double,
        isMemberPayingMe: Boolean,
        groupExpenses: List<ExpenseWithSplits>
    ) {
        if (settledAmount <= 0.01) return
        val currentUser = authRepository.currentUserName

        var remainingToAllocate = settledAmount
        val sortedOldestFirst = groupExpenses
            .filter { !it.expense.isDeleted }
            .sortedBy { it.expense.updatedAt }

        for (item in sortedOldestFirst) {
            if (remainingToAllocate <= 0.01) break

            val targetSplit = if (isMemberPayingMe && item.expense.paidByUserId == currentUser) {
                item.splits.find { it.userId == memberName && !it.isSettled && it.amountOwed > 0.01 }
            } else if (!isMemberPayingMe && item.expense.paidByUserId == memberName) {
                item.splits.find { it.userId == currentUser && !it.isSettled && it.amountOwed > 0.01 }
            } else {
                null
            }

            val dateLabel = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                .format(java.util.Date())

            if (targetSplit != null) {
                if (remainingToAllocate >= targetSplit.amountOwed - 0.01) {
                    val paidNow = targetSplit.amountOwed
                    remainingToAllocate -= paidNow
                    expenseDao.applyPaymentToSplit(
                        splitId = targetSplit.id,
                        remainingAmount = 0.0,
                        justPaidAmount = paidNow,
                        historyEntry = "₹${"%.2f".format(paidNow)} ($dateLabel)",
                        isFullySettled = true
                    )
                } else {
                    val paidNow = remainingToAllocate
                    val newRemaining = targetSplit.amountOwed - paidNow
                    remainingToAllocate = 0.0
                    expenseDao.applyPaymentToSplit(
                        splitId = targetSplit.id,
                        remainingAmount = newRemaining,
                        justPaidAmount = paidNow,
                        historyEntry = "₹${"%.2f".format(paidNow)} ($dateLabel)",
                        isFullySettled = false
                    )
                }
            }
        }

        triggerBackgroundSync()
    }


}