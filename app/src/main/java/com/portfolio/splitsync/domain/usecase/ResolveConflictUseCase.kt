package com.portfolio.splitsync.domain.usecase

import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.domain.model.SyncStatus
import javax.inject.Inject

sealed class ConflictResolutionResult {
    data class KeepLocal(val mergedExpense: ExpenseEntity) : ConflictResolutionResult()
    data class AcceptRemote(val remoteExpense: ExpenseEntity) : ConflictResolutionResult()
    data class NeedsManualReview(
        val local: ExpenseEntity,
        val remote: ExpenseEntity
    ) : ConflictResolutionResult()
}

class ResolveConflictUseCase @Inject constructor() {

    fun resolve(local: ExpenseEntity, remote: ExpenseEntity): ConflictResolutionResult {
        if (local.syncStatus == SyncStatus.PENDING_DELETE || local.isDeleted) {
            val nextVersion = maxOf(local.version, remote.version) + 1
            return ConflictResolutionResult.KeepLocal(
                local.copy(
                    isDeleted = true,
                    version = nextVersion,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
            )
        }

        if (remote.isDeleted) {
            return ConflictResolutionResult.AcceptRemote(
                remote.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED)
            )
        }

        if (local.syncStatus == SyncStatus.SYNCED) {
            return ConflictResolutionResult.AcceptRemote(remote.copy(syncStatus = SyncStatus.SYNCED))
        }

        // 4. Financial amount collision check
        if (local.version != remote.version && local.amount != remote.amount) {
            return ConflictResolutionResult.NeedsManualReview(
                local = local.copy(syncStatus = SyncStatus.CONFLICT),
                remote = remote
            )
        }

        return if (local.updatedAt >= remote.updatedAt) {
            val nextVersion = maxOf(local.version, remote.version) + 1
            ConflictResolutionResult.KeepLocal(
                local.copy(version = nextVersion, syncStatus = SyncStatus.PENDING_UPDATE)
            )
        } else {
            ConflictResolutionResult.AcceptRemote(
                remote.copy(syncStatus = SyncStatus.SYNCED)
            )
        }
    }
}
