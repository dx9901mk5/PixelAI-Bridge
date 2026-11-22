package com.dx9901mk5.pixelaibridge.core.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY
}

/**
 * Manages Shizuku binder lifecycle and permission state for optional rootless enhancements.
 */
class ShizukuBridge(context: Context) {

    private val appContext = context.applicationContext
    private val _status = MutableStateFlow(evaluateStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                refreshStatus()
            }
        }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun refreshStatus() {
        _status.value = evaluateStatus()
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            refreshStatus()
            return
        }
        if (Shizuku.isPreV11()) {
            refreshStatus()
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } else {
            refreshStatus()
        }
    }

    private fun evaluateStatus(): ShizukuStatus {
        if (!isShizukuInstalled()) return ShizukuStatus.NOT_INSTALLED
        if (!Shizuku.pingBinder()) return ShizukuStatus.NOT_RUNNING
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuStatus.READY
        } else {
            ShizukuStatus.PERMISSION_REQUIRED
        }
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE_SHIZUKU = 9001
    }
}
