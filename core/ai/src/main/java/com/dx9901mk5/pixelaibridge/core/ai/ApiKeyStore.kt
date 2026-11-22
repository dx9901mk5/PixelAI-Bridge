package com.dx9901mk5.pixelaibridge.core.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the user's BYOK Gemini API key and model preference securely on-device.
 */
class ApiKeyStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    private val _hasApiKey = MutableStateFlow(getApiKey().isNotBlank())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(loadValidModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private fun loadValidModel(): String {
        val saved = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        return if (saved in SUPPORTED_MODELS) {
            saved
        } else {
            prefs.edit().putString(KEY_MODEL, DEFAULT_MODEL).apply()
            DEFAULT_MODEL
        }
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun saveApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _hasApiKey.value = trimmed.isNotBlank()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _hasApiKey.value = false
    }

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _selectedModel.value = model
    }

    private fun createPrefs(appContext: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback for devices with broken Keystore implementations.
            appContext.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_NAME = "pixelai_secure_prefs"
        private const val PREFS_NAME_FALLBACK = "pixelai_prefs_fallback"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        val SUPPORTED_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-3.5-flash-lite"
        )
    }
}
