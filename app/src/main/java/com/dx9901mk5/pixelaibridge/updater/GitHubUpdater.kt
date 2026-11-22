package com.dx9901mk5.pixelaibridge.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.dx9901mk5.pixelaibridge.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val body: String,
    val apkDownloadUrl: String?,
    val htmlUrl: String,
    val hasUpdate: Boolean
)

sealed interface UpdateCheckResult {
    data class Success(val info: ReleaseInfo) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Checks GitHub Releases for updates, downloads the APK asset, and launches Android's installer.
 */
class GitHubUpdater(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun checkLatestRelease(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "PixelAI-Bridge/${BuildConfig.VERSION_NAME}")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val hint = if (response.code == 404) {
                        "GitHub 尚未發佈任何 Release"
                    } else {
                        "HTTP ${response.code}"
                    }
                    return@withContext UpdateCheckResult.Error(hint)
                }

                val json = JSONObject(raw)
                val tag = json.optString("tag_name")
                val title = json.optString("name").ifBlank { tag }
                val body = json.optString("body")
                val htmlUrl = json.optString("html_url").ifBlank { REPO_URL }

                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                UpdateCheckResult.Success(
                    ReleaseInfo(
                        tagName = tag,
                        title = title,
                        body = body,
                        apkDownloadUrl = apkUrl,
                        htmlUrl = htmlUrl,
                        hasUpdate = isNewerVersion(tag, BuildConfig.VERSION_NAME)
                    )
                )
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "檢查更新失敗")
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        val downloadResult = downloadApkOnly(context, downloadUrl, onProgress)
        return downloadResult.mapCatching { apkFile ->
            withContext(Dispatchers.Main) {
                launchApkInstaller(context, apkFile)
            }
        }
    }

    suspend fun downloadApkOnly(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "PixelAI-Bridge-update.apk")

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "PixelAI-Bridge/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("下載失敗 HTTP ${response.code}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("伺服器未回傳檔案內容"))
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buf = ByteArray(16 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            downloaded += read
                            if (totalBytes > 0L) {
                                onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildInstallIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File) {
        context.startActivity(buildInstallIntent(context, apkFile))
    }

    fun isAutoUpdateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_UPDATE, true)
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
    }

    internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = parseSemver(remoteTag)
        val currentParts = parseSemver(currentVersion)
        for (i in 0 until 3) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseSemver(raw: String): List<Int> {
        return raw.removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
    }

    companion object {
        const val REPO_URL = "https://github.com/dx9901mk5/PixelAI-Bridge"
        private const val PREFS_NAME = "pixelai_updater_prefs"
        private const val KEY_AUTO_UPDATE = "auto_update_enabled"
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/dx9901mk5/PixelAI-Bridge/releases/latest"
    }
}
