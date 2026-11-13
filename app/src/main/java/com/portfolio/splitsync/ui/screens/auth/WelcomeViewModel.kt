package com.portfolio.splitsync.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.splitsync.domain.repository.AuthRepository
import com.portfolio.splitsync.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WelcomeUiState(isAuthenticated = authRepository.isUserLoggedIn)
    )
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(nameInput = newName, errorMessage = null) }
    }

    fun onPhoneChanged(newPhone: String) {
        val digitsOnly = newPhone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phoneInput = digitsOnly, errorMessage = null) }
    }

    fun continueWithProfile() {
        val name = _uiState.value.nameInput.trim()
        val phone = _uiState.value.phoneInput.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your display name") }
            return
        }
        if (phone.length != 10) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid 10-digit mobile number") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithNameAndPhone(name, phone)
            result.onSuccess {
                expenseRepository.syncGroupsForUserPhone(phone, name)
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Authentication failed"
                    )
                }
            }
        }
    }
}
