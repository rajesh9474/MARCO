package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ApiKeyManager {
    private const val PREFS_NAME = "marco_api_key_prefs"
    private const val KEY_GEMINI = "user_gemini_api_key"

    private var prefs: SharedPreferences? = null

    private val _apiKeySource = MutableStateFlow<String>("Checking...")
    val apiKeySource: StateFlow<String> = _apiKeySource.asStateFlow()

    private val _isConfigured = MutableStateFlow<Boolean>(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        refreshStatus()
    }

    fun getApiKey(): String {
        // 1. Check user custom entered key first
        val customKey = prefs?.getString(KEY_GEMINI, "")?.trim() ?: ""
        if (customKey.isNotBlank() && customKey != "MY_GEMINI_API_KEY") {
            return customKey
        }
        // 2. Fall back to BuildConfig.GEMINI_API_KEY from Secrets / .env
        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }.trim()
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun getCustomApiKey(): String {
        return prefs?.getString(KEY_GEMINI, "") ?: ""
    }

    fun saveCustomApiKey(key: String) {
        prefs?.edit()?.putString(KEY_GEMINI, key.trim())?.apply()
        refreshStatus()
    }

    fun clearCustomApiKey() {
        prefs?.edit()?.remove(KEY_GEMINI)?.apply()
        refreshStatus()
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    fun refreshStatus() {
        val customKey = prefs?.getString(KEY_GEMINI, "")?.trim() ?: ""
        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }.trim()

        if (customKey.isNotBlank() && customKey != "MY_GEMINI_API_KEY") {
            _apiKeySource.value = "Custom Key (In-App Settings)"
            _isConfigured.value = true
        } else if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            _apiKeySource.value = "AI Studio Secrets (BuildConfig)"
            _isConfigured.value = true
        } else {
            _apiKeySource.value = "Not Configured"
            _isConfigured.value = false
        }
    }
}
