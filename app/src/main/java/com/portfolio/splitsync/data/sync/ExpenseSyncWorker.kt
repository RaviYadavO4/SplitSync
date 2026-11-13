package com.portfolio.splitsync.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.portfolio.splitsync.data.local.dao.ExpenseDao
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.domain.model.SyncStatus
import com.portfolio.splitsync.domain.usecase.ConflictResolutionResult
import com.portfolio.splitsync.domain.usecase.ResolveConflictUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class ExpenseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val firestore: FirebaseFirestore,
    private val resolveConflictUseCase: ResolveConflictUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Unsynced groups ko Firestore par push karein
            val unsyncedGroups = expenseDao.getUnsyncedGroups()
            for (group in unsyncedGroups) {
                val phoneList = group.memberPhones.values.filter { it.length == 10 }.distinct()
                val groupPayload = mapOf(
                    "id" to group.id,
                    "name" to group.name,
                    "description" to group.description,
                    "inviteCode" to group.inviteCode,
                    "currencySymbol" to group.currencySymbol,
                    "memberIds" to group.memberIds,
                    "memberPhones" to group.memberPhones,
                    "phoneList" to phoneList,
                    "createdBy" to group.createdBy,
                    "updatedAt" to group.updatedAt,
                    "version" to group.version,
                    "syncStatus" to SyncStatus.SYNCED.name,
                    "isDeleted" to group.isDeleted
                )
                firestore.collection("groups")
                    .document(group.id)
                    .set(groupPayload)
                    .await()
                expenseDao.updateGroupSyncStatus(group.id, SyncStatus.SYNCED)
            }

            // 2. Unsynced expenses ko sync aur conflict resolve karein
            val unsyncedExpenses = expenseDao.getUnsyncedExpenses()
            for (localExpense in unsyncedExpenses) {
                syncSingleExpense(localExpense)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun syncSingleExpense(local: ExpenseEntity) {
        val docRef = firestore.collection("groups")
            .document(local.groupId)
            .collection("expenses")
            .document(local.id)

        val remoteSnapshot = docRef.get().await()

        if (!remoteSnapshot.exists()) {
            val syncedItem = local.copy(syncStatus = SyncStatus.SYNCED)
            docRef.set(syncedItem).await()
            expenseDao.updateSyncState(local.id, SyncStatus.SYNCED, local.version)
        } else {
            val remoteVersion = remoteSnapshot.getLong("version")?.toInt() ?: 1
            val remoteAmount = remoteSnapshot.getDouble("amount") ?: 0.0
            val remoteUpdatedAt = remoteSnapshot.getLong("updatedAt") ?: 0L
            val remoteTitle = remoteSnapshot.getString("title") ?: local.title
            val remoteIsDeleted = remoteSnapshot.getBoolean("isDeleted") ?: false

            val remoteEntity = local.copy(
                title = remoteTitle,
                amount = remoteAmount,
                updatedAt = remoteUpdatedAt,
                version = remoteVersion,
                isDeleted = remoteIsDeleted,
                syncStatus = SyncStatus.SYNCED
            )

            when (val resolution = resolveConflictUseCase.resolve(local, remoteEntity)) {
                is ConflictResolutionResult.KeepLocal -> {
                    val updated = resolution.mergedExpense.copy(syncStatus = SyncStatus.SYNCED)
                    docRef.set(updated).await()
                    expenseDao.updateExpenseOnly(updated)
                }
                is ConflictResolutionResult.AcceptRemote -> {
                    expenseDao.updateExpenseOnly(resolution.remoteExpense)
                }
                is ConflictResolutionResult.NeedsManualReview -> {
                    expenseDao.updateExpenseOnly(resolution.local)
                }
            }
        }
    }
}
