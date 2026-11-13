package com.portfolio.splitsync.ui.screens.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.domain.model.SplitType
import com.portfolio.splitsync.domain.repository.AuthRepository
import com.portfolio.splitsync.domain.repository.ExpenseRepository
import com.portfolio.splitsync.domain.usecase.CalculateNetBalanceUseCase
import com.portfolio.splitsync.domain.usecase.CalculateSplitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val calculateSplitUseCase: CalculateSplitUseCase,
    private val calculateNetBalanceUseCase: CalculateNetBalanceUseCase
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val _activityMessage = MutableStateFlow<String?>(null)
    private var previousMembersSnapshot: Set<String>? = null
    private var lastDeletedExpenseId: String? = null

    init {
        repository.observeRealtimeGroupSync(groupId).launchIn(viewModelScope)
    }

    val uiState: StateFlow<GroupDetailUiState> = combine(
        repository.observeGroupById(groupId),
        repository.observeGroupExpenses(groupId),
        _activityMessage
    ) { group, expenses, activityMsg ->
        val currentUser = authRepository.currentUserName
        val members = group?.memberIds ?: listOf(currentUser)
        val phonesMap = group?.memberPhones ?: emptyMap()

        // Detect newly joined members
        val currentSet = members.toSet()
        val prev = previousMembersSnapshot
        if (prev != null && currentSet.size > prev.size) {
            val newlyJoined = (currentSet - prev).joinToString(", ")
            _activityMessage.value = "$newlyJoined joined this group!"
        }
        previousMembersSnapshot = currentSet

        // Calculate per-member net balances via UseCase
        val balanceMap = calculateNetBalanceUseCase.calculateGroupMemberBalances(
            currentUser = currentUser,
            groupExpenses = expenses
        )

        val balancesList = members
            .filter { it != currentUser }
            .map { member ->
                MemberBalance(
                    memberName = member,
                    phone = phonesMap[member] ?: "",
                    netAmount = balanceMap[member] ?: 0.0
                )
            }

        // Category-wise spending analytics
        val activeNonSettlement = expenses.filter { it.expense.category != "Settlement" }
        val categoryMap = activeNonSettlement
            .groupBy { it.expense.category }
            .mapValues { entry -> entry.value.sumOf { it.expense.amount } }

        GroupDetailUiState(
            groupId = groupId,
            groupName = group?.name ?: "Group Details",
            inviteCode = group?.inviteCode ?: "",
            currentUserName = currentUser,
            members = members,
            expenses = expenses,
            totalGroupSpend = activeNonSettlement.sumOf { it.expense.amount },
            categoryTotals = categoryMap,
            memberBalances = balancesList,
            recentActivityMessage = activityMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupDetailUiState(groupId = groupId)
    )

    fun addMember(memberName: String, rawPhone: String = "") {
        val clean = memberName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.addMemberToGroup(groupId, clean, rawPhone)
            val cleanPhone = rawPhone.filter { it.isDigit() }
            val suffix = if (cleanPhone.length == 10) " (+91 $cleanPhone)" else ""
            _activityMessage.value = "Added '$clean'$suffix to ${uiState.value.groupName}"
        }
    }

    fun clearActivityMessage() {
        _activityMessage.value = null
    }

    fun addExpenseToThisGroup(
        title: String,
        amount: Double,
        category: String,
        paidBy: String,
        splitType: SplitType,
        customSplits: Map<String, Double>
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUserName
            val currentMembers = uiState.value.members.ifEmpty { listOf(currentUser) }

            val expense = ExpenseEntity(
                groupId = groupId,
                title = title.trim(),
                amount = amount,
                paidByUserId = paidBy,
                splitType = splitType,
                category = category,
                lastModifiedBy = currentUser
            )

            val splits = calculateSplitUseCase(
                expenseId = expense.id,
                totalAmount = amount,
                members = currentMembers,
                paidBy = paidBy,
                splitType = splitType,
                customSplits = customSplits
            )

            repository.addOrUpdateExpense(expense, splits)
        }
    }

    fun deleteExpense(expenseId: String, title: String) {
        lastDeletedExpenseId = expenseId
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _activityMessage.value = "Deleted '$title'"
        }
    }

    fun undoLastDelete() {
        val idToRestore = lastDeletedExpenseId ?: return
        lastDeletedExpenseId = null
        viewModelScope.launch {
            repository.restoreDeletedExpense(idToRestore)
        }
    }

    fun deleteGroup(onGroupDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGroup(groupId)
            onGroupDeleted()
        }
    }

    fun recordSettlementPayment(
        memberName: String,
        settledAmount: Double,
        isMemberPayingMe: Boolean
    ) {
        if (settledAmount <= 0.01) return

        viewModelScope.launch {
            repository.recordFifoSettlement(
                groupId = groupId,
                memberName = memberName,
                settledAmount = settledAmount,
                isMemberPayingMe = isMemberPayingMe,
                groupExpenses = uiState.value.expenses
            )
            _activityMessage.value = "Recorded ₹${"%.2f".format(settledAmount)} settlement with $memberName"
        }
    }

    fun resolveConflict(expense: ExpenseEntity, keepLocal: Boolean) {
        viewModelScope.launch {
            repository.resolveExpenseConflict(expense, keepLocal)
        }
    }

    fun generateShareableReportText(): String {
        val s = uiState.value
        return buildString {
            appendLine("📊 *${s.groupName} — Expense Report*")
            appendLine("Total Group Spend: ₹${"%.2f".format(s.totalGroupSpend)}")
            appendLine("Members: ${s.members.joinToString(", ")}")
            appendLine()
            appendLine("📁 *Category Breakdown:*")
            s.categoryTotals.forEach { (cat, amt) ->
                appendLine("• $cat: ₹${"%.2f".format(amt)}")
            }
            appendLine()
            appendLine("💸 *Net Balances (w.r.t ${s.currentUserName}):*")
            s.memberBalances.forEach { bal ->
                val status = when {
                    bal.netAmount > 0.01 -> "Owes ${s.currentUserName} ₹${"%.2f".format(bal.netAmount)}"
                    bal.netAmount < -0.01 -> "${s.currentUserName} owes ₹${"%.2f".format(-bal.netAmount)}"
                    else -> "Settled ✓"
                }
                appendLine("• ${bal.memberName}: $status")
            }
            appendLine("\nGenerated via SplitSync")
        }
    }
}
