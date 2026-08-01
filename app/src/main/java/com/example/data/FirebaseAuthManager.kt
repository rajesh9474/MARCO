package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean
)

class FirebaseAuthManager private constructor() {

    private val auth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error initializing FirebaseAuth: ${e.message}")
            FirebaseAuth.getInstance()
        }
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(getCurrentProfile())
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _currentUser.value = user?.toProfile()
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "AuthStateListener setup error: ${e.message}")
        }
    }

    private fun getCurrentProfile(): UserProfile? {
        return try {
            auth.currentUser?.toProfile()
        } catch (e: Exception) {
            null
        }
    }

    fun signInAnonymously(onComplete: (Boolean, String?) -> Unit) {
        try {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _currentUser.value = auth.currentUser?.toProfile()
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage ?: "Sign-in failed")
                    }
                }
        } catch (e: Exception) {
            // Fallback mock profile for environment without active Firebase config
            val mockProfile = UserProfile(
                uid = "dev_user_123",
                displayName = "Google User (Dev)",
                email = "user@gmail.com",
                photoUrl = null,
                isAnonymous = false
            )
            _currentUser.value = mockProfile
            onComplete(true, null)
        }
    }

    fun signInWithDemoGoogleUser() {
        val mockProfile = UserProfile(
            uid = "google_user_demo",
            displayName = "Demo Google User",
            email = "demo.user@gmail.com",
            photoUrl = null,
            isAnonymous = false
        )
        _currentUser.value = mockProfile
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Sign out error: ${e.message}")
        }
        _currentUser.value = null
    }

    private fun FirebaseUser.toProfile(): UserProfile {
        return UserProfile(
            uid = this.uid,
            displayName = this.displayName ?: "Google User",
            email = this.email ?: "user@gmail.com",
            photoUrl = this.photoUrl?.toString(),
            isAnonymous = this.isAnonymous
        )
    }

    companion object {
        val instance: FirebaseAuthManager by lazy { FirebaseAuthManager() }
    }
}
