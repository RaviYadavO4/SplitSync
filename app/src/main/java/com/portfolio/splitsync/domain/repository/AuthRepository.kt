package com.portfolio.splitsync.domain.repository

interface AuthRepository {
    val isUserLoggedIn: Boolean
    val currentUserId: String
    val currentUserName: String
    val currentUserPhone: String

    fun normalizePhone(rawPhone: String): String
    suspend fun signInWithNameAndPhone(displayName: String, rawPhone: String): Result<Unit>
    fun signOut()
}
