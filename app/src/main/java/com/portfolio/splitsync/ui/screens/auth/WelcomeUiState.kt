package com.portfolio.splitsync.ui.screens.auth

data class WelcomeUiState(
    val nameInput: String = "",
    val phoneInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)
