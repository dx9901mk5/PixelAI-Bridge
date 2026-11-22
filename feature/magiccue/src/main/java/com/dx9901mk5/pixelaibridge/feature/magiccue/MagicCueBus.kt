package com.dx9901mk5.pixelaibridge.feature.magiccue

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CapturedCueNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestampMs: Long
)

/**
 * In-memory ring buffer and privacy exclusion store for Magic Cue notifications.
 */
object MagicCueBus {
    private val _notifications = MutableStateFlow<List<CapturedCueNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedCueNotification>> = _notifications.asStateFlow()

    private val defaultExcludedKeywords = listOf(
        "bank", "wallet", "pay", "authenticator", "1password", "bitwarden", "keepass", "android"
    )

    fun isPackageAllowed(context: Context, packageName: String): Boolean {
        val lower = packageName.lowercase()
        if (lower == context.packageName.lowercase()) return false
        if (defaultExcludedKeywords.any { lower.contains(it) }) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userExcluded = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        return packageName !in userExcluded
    }

    fun getExcludedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun addExcludedPackage(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = HashSet(getExcludedPackages(context)).apply { add(packageName) }
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, updated).apply()
        _notifications.update { list -> list.filterNot { it.packageName == packageName } }
    }

    fun publish(item: CapturedCueNotification) {
        _notifications.update { current ->
            (listOf(item) + current)
                .distinctBy { "${it.packageName}:${it.title}:${it.text}" }
                .take(30)
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    private const val PREFS_NAME = "pixelai_magiccue_prefs"
    private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
}
