package com.dx9901mk5.pixelaibridge.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Periodically checks GitHub Releases for updates, downloads the APK automatically,
 * and posts a tap-to-install notification.
 */
class AutoUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val updater = GitHubUpdater()
        if (!updater.isAutoUpdateEnabled(applicationContext)) {
            return Result.success()
        }

        val check = updater.checkLatestRelease()
        if (check is UpdateCheckResult.Success && check.info.hasUpdate) {
            val apkUrl = check.info.apkDownloadUrl ?: return Result.success()
            val downloaded = updater.downloadApkOnly(applicationContext, apkUrl)
            downloaded.onSuccess { apkFile ->
                postInstallReadyNotification(applicationContext, check.info.tagName, apkFile)
            }
        }
        return Result.success()
    }

    private fun postInstallReadyNotification(
        context: Context,
        versionTag: String,
        apkFile: File
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "自動更新通知",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val installIntent = GitHubUpdater().buildInstallIntent(context, apkFile)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("PixelAI Bridge $versionTag 已下載完成")
            .setContentText("點擊立即進入系統安裝程式")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "pixelai_auto_update_check"
        private const val CHANNEL_ID = "pixelai_updates"
        private const val NOTIFICATION_ID = 4021

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
