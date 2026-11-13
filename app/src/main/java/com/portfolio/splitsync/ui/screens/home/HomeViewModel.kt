package com.portfolio.splitsync.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.data.local.entity.GroupEntity
import com.portfolio.splitsync.data.sync.NetworkMonitor
import com.portfolio.splitsync.domain.repository.AuthRepository
import com.portfolio.splitsync.domain.repository.ExpenseRepository
import com.portfolio.splitsync.domain.usecase.CalculateNetBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val calculateNetBalanceUseCase: CalculateNetBalanceUseCase,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _joinMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        networkMonitor.isOnline,
        repository.observePendingSyncCount(),
        repository.observeGroups(),
        repository.observeAllExpenses(),
        repository.observeAllSplits(),
        _joinMessage
    ) { values ->
        val isOnline = values[0] as Boolean
        val pendingCount = values[1] as Int
        @Suppress("UNCHECKED_CAST")
        val groups = values[2] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[3] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splits = values[4] as List<ExpenseSplitEntity>
        val joinMsg = values[5] as String?

        val currentUser = authRepository.currentUserName
        val balanceSummary = calculateNetBalanceUseCase.calculateGlobalSummary(
            currentUser = currentUser,
            expenses = expenses,
            splits = splits
        )

        HomeUiState(
            currentUserName = currentUser,
            currentUserPhone = authRepository.currentUserPhone,
            isOnline = isOnline,
            pendingSyncCount = pendingCount,
            groups = groups,
            youAreOwed = balanceSummary.youAreOwed,
            youOwe = balanceSummary.youOwe,
            joinMessage = joinMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun createNewGroup(
        name: String,
        description: String,
        membersWithPhones: Map<String, String>
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUserName
            val currentPhone = authRepository.currentUserPhone

            val finalPhonesMap = mutableMapOf<String, String>()
            if (currentPhone.length == 10) {
                finalPhonesMap[currentUser] = currentPhone
            }

            val otherMembers = mutableListOf<String>()
            for ((memberName, rawPhone) in membersWithPhones) {
                val cleanName = memberName.trim()
                if (cleanName.isNotBlank() && !cleanName.equals(currentUser, ignoreCase = true)) {
                    otherMembers.add(cleanName)
                    val cleanPhone = authRepository.normalizePhone(rawPhone)
                    if (cleanPhone.length == 10) {
                        finalPhonesMap[cleanName] = cleanPhone
                    }
                }
            }

            val finalMembers = (listOf(currentUser) + otherMembers).distinct()

            repository.createGroup(
                GroupEntity(
                    name = name.trim(),
                    description = description.trim().ifEmpty { "Shared Expenses" },
                    memberIds = finalMembers,
                    memberPhones = finalPhonesMap,
                    createdBy = currentUser
                )
            )
        }
    }

    fun joinGroupByCode(code: String) {
        viewModelScope.launch {
            val result = repository.joinGroupByInviteCode(code)
            result.onSuccess { groupName ->
                _joinMessage.value = "Joined '$groupName' successfully!"
            }.onFailure { err ->
                _joinMessage.value = err.message ?: "Failed to join group"
            }
        }
    }

    fun clearJoinMessage() {
        _joinMessage.value = null
    }

    fun logout(onLoggedOut: () -> Unit) {
        authRepository.signOut()
        onLoggedOut()
    }
}
