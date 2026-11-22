package com.dx9901mk5.pixelaibridge.feature.screenshots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * Runs on-device ML Kit OCR (Chinese + Latin) and prepares scaled JPEGs for Gemini Vision.
 */
class ScreenshotOcrEngine(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeText(uri: Uri): String = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (_: Exception) {
            return@withContext ""
        }
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result.text.trim())
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
        }
    }

    suspend fun loadScaledJpeg(uri: Uri, maxDimension: Int = 1024): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
                val largest = max(boundsOpts.outWidth, boundsOpts.outHeight)
                val sampleSize = if (largest > maxDimension) {
                    (largest / maxDimension).coerceAtLeast(1)
                } else {
                    1
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: return@withContext null

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
                bitmap.recycle()
                out.toByteArray()
            } catch (_: Exception) {
                null
            }
        }
}
