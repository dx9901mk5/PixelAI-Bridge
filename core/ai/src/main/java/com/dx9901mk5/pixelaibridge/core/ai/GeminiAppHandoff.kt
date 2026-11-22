package com.dx9901mk5.pixelaibridge.core.ai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Bridges prompts, OCR text, and images directly to the installed Gemini app without requiring an API key.
 */
object GeminiAppHandoff {

    private const val GEMINI_PACKAGE = "com.google.android.apps.bard"

    fun isGeminiAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GEMINI_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun sendText(context: Context, prompt: String): Boolean {
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchTargetedOrChooser(context, baseIntent, "傳送給 Gemini")
    }

    fun sendImageWithPrompt(context: Context, imageUri: Uri, prompt: String): Boolean {
        return sendMediaWithPrompt(context, imageUri, "image/*", prompt, "傳送圖片給 Gemini")
    }

    fun sendAudioWithPrompt(context: Context, audioUri: Uri, prompt: String): Boolean {
        return sendMediaWithPrompt(context, audioUri, "audio/*", prompt, "傳送音訊給 Gemini")
    }

    fun launchGoogleSoundSearch(context: Context): Boolean {
        val soundSearch = Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(soundSearch) }.isSuccess) {
            return true
        }
        return sendText(context, "請幫我聽聽看現在播放的是什麼歌曲，或讓我用哼唱辨識。")
    }

    private fun sendMediaWithPrompt(
        context: Context,
        mediaUri: Uri,
        mimeType: String,
        prompt: String,
        chooserTitle: String
    ): Boolean {
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, mediaUri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchTargetedOrChooser(context, baseIntent, chooserTitle)
    }

    private fun launchTargetedOrChooser(
        context: Context,
        intent: Intent,
        chooserTitle: String
    ): Boolean {
        if (isGeminiAppInstalled(context)) {
            val targeted = Intent(intent).setPackage(GEMINI_PACKAGE)
            val launched = runCatching { context.startActivity(targeted) }.isSuccess
            if (launched) return true
        }
        return runCatching {
            val chooser = Intent.createChooser(intent, chooserTitle)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
