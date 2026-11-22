package com.dx9901mk5.pixelaibridge.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.dx9901mk5.pixelaibridge.feature.magiccue.MagicCueNotificationListener

data class PermissionItemState(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val runtimePermission: String? = null
)

object PermissionCenter {

    fun collectStatuses(context: Context): List<PermissionItemState> {
        val mediaPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val mediaGranted = ContextCompat.checkSelfPermission(context, mediaPerm) ==
            PackageManager.PERMISSION_GRANTED

        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
        val notifGranted = if (notifPerm != null) {
            ContextCompat.checkSelfPermission(context, notifPerm) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val listenerGranted = isNotificationListenerEnabled(context)
        val overlayGranted = Settings.canDrawOverlays(context)
        val batteryGranted = isIgnoringBatteryOptimizations(context)
        val installGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        return listOf(
            PermissionItemState(
                id = "media",
                title = "媒體與截圖讀取",
                description = "AI 截圖記憶庫掃描與相簿讀取",
                isGranted = mediaGranted,
                runtimePermission = mediaPerm
            ),
            PermissionItemState(
                id = "mic",
                title = "麥克風錄音",
                description = "Now Playing 識曲與 Call Notes 即時錄音",
                isGranted = micGranted,
                runtimePermission = Manifest.permission.RECORD_AUDIO
            ),
            PermissionItemState(
                id = "post_notif",
                title = "系統通知顯示",
                description = "顯示識曲與背景處理狀態通知",
                isGranted = notifGranted,
                runtimePermission = notifPerm
            ),
            PermissionItemState(
                id = "notif_listener",
                title = "通知存取權 (Magic Cue)",
                description = "擷取來訊通知以產生智慧語境建議",
                isGranted = listenerGranted
            ),
            PermissionItemState(
                id = "overlay",
                title = "顯示在其他應用程式上層",
                description = "懸浮快捷建議與跨 App 輔助顯示",
                isGranted = overlayGranted
            ),
            PermissionItemState(
                id = "battery",
                title = "忽略電池最佳化",
                description = "防止陸版 ROM 殺後台導致監聽中斷",
                isGranted = batteryGranted
            ),
            PermissionItemState(
                id = "install_unknown",
                title = "允許安裝未知應用程式",
                description = "用於應用內自動下載更新並進入安裝",
                isGranted = installGranted
            )
        )
    }

    fun openPermissionSettings(context: Context, id: String) {
        val pkgUri = Uri.parse("package:${context.packageName}")
        val intent = when (id) {
            "notif_listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            "overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)
            "battery" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri)
            "install_unknown" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
            }
            "post_notif" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
            }
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        val cn = ComponentName(context, MagicCueNotificationListener::class.java).flattenToString()
        return flat.contains(cn)
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
