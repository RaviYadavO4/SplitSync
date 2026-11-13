package com.portfolio.splitsync.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.portfolio.splitsync.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): AuthRepository {
    private val prefs = context.getSharedPreferences("splitsync_auth_prefs", Context.MODE_PRIVATE)
    override val isUserLoggedIn: Boolean
        get() = currentUserName != "You" && currentUserPhone.length == 10



    override val currentUserId: String
        get() = firebaseAuth.currentUser?.uid
            ?: prefs.getString("local_uid", "guest_uid")
            ?: "guest_uid"

    override val currentUserName: String
        get() = prefs.getString("user_name", null)
            ?: firebaseAuth.currentUser?.displayName
            ?: "You"

    override val currentUserPhone: String
        get() = prefs.getString("user_phone", "") ?: ""

    override fun normalizePhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    override suspend fun signInWithNameAndPhone(displayName: String, rawPhone: String): Result<Unit> {
        val cleanName = displayName.trim()
        val normalizedPhone = normalizePhone(rawPhone)

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your name"))
        }
        if (normalizedPhone.length != 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number"))
        }

        // 1. Pehle local session save karein taaki offline login bhi turant kaam kare
        val fallbackUid = prefs.getString("local_uid", null) ?: UUID.randomUUID().toString()
        prefs.edit()
            .putString("local_uid", fallbackUid)
            .putString("user_name", cleanName)
            .putString("user_phone", normalizedPhone)
            .apply()

        // 2. Online hone par Firebase Auth & Firestore directory update karein
        try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val uid = authResult.user?.uid ?: fallbackUid

            val profileUpdates = userProfileChangeRequest {
                this.displayName = cleanName
            }
            authResult.user?.updateProfile(profileUpdates)?.await()

            prefs.edit().putString("local_uid", uid).apply()

            val userMap = mapOf(
                "uid" to uid,
                "name" to cleanName,
                "phone" to normalizedPhone,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(normalizedPhone).set(userMap).await()
        } catch (_: Exception) {
            // Agar user offline hai toh local session pehle hi save ho chuka hai, isliye error nahi dikhayenge
        }

        return Result.success(Unit)
    }

    override fun signOut() {
        prefs.edit().clear().apply()
        firebaseAuth.signOut()
    }
}
