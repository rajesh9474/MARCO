package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FirestoreChatMessage(
    val id: String = "",
    val userId: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = ""
)

class FirestoreManager private constructor() {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Firestore init error: ${e.message}")
            null
        }
    }

    suspend fun saveChatMessage(userId: String, sender: String, text: String, modelUsed: String = "gemini-3.5-flash") = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val docRef = db.collection("users").document(userId).collection("conversations").document()
            val data = hashMapOf(
                "id" to docRef.id,
                "userId" to userId,
                "sender" to sender,
                "text" to text,
                "timestamp" to System.currentTimeMillis(),
                "modelUsed" to modelUsed
            )
            docRef.set(data)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to save to Firestore: ${e.message}")
        }
    }

    suspend fun saveUserPreference(userId: String, key: String, value: Any) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val db = firestore ?: return@withContext
            val docRef = db.collection("users").document(userId).collection("settings").document("user_prefs")
            docRef.set(mapOf(key to value), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to save preference to Firestore: ${e.message}")
        }
    }

    companion object {
        val instance: FirestoreManager by lazy { FirestoreManager() }
    }
}
