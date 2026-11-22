package com.dx9901mk5.pixelaibridge.feature.studio

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dx9901mk5.pixelaibridge.core.ai.GeminiAppHandoff
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelStudioScreen(
    padding: PaddingValues,
    geminiClient: GeminiClient,
    hasApiKey: Boolean,
    initialSharedImageUri: Uri? = null,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var themeInput by remember { mutableStateOf("莫奈晨霧森林") }
    var wallpaperBitmap by remember {
        mutableStateOf(
            renderPixelWallpaper(
                width = 540,
                height = 960,
                colors = listOf(0xFF0B57D0.toInt(), 0xFF7FCFFF.toInt(), 0xFFC4EED0.toInt()),
                seed = 42
            )
        )
    }
    var paletteCaption by remember { mutableStateOf("預設莫奈漸層 · 可由 Gemini 設計專屬配色") }
    var isGeneratingWallpaper by remember { mutableStateOf(false) }

    var selectedImageUri by remember(initialSharedImageUri) { mutableStateOf(initialSharedImageUri) }
    var selectedStylePreset by remember { mutableStateOf("日系底片") }
    val stylePresets = remember { listOf("日系底片", "賽博霓虹", "去除路人", "立體貼圖風", "極簡構圖") }
    var imageAnalysis by remember { mutableStateOf<String?>(null) }
    var isAnalyzingImage by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            imageAnalysis = null
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            PixelSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null)
                    Text(
                        text = "Pixel AI 動態桌布工作室",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = themeInput,
                    onValueChange = { themeInput = it },
                    label = { Text("桌布意境 / 主題關鍵字") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    bitmap = wallpaperBitmap.asImageBitmap(),
                    contentDescription = "Wallpaper Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = paletteCaption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !isGeneratingWallpaper,
                        onClick = {
                            scope.launch {
                                isGeneratingWallpaper = true
                                val (colors, caption) = if (hasApiKey && themeInput.isNotBlank()) {
                                    generatePaletteWithGemini(geminiClient, themeInput)
                                } else {
                                    randomMonetPalette() to "離線隨機莫奈幾何配色"
                                }
                                wallpaperBitmap = withContext(Dispatchers.Default) {
                                    renderPixelWallpaper(
                                        width = 1080,
                                        height = 2400,
                                        colors = colors,
                                        seed = System.currentTimeMillis().toInt()
                                    )
                                }
                                paletteCaption = caption
                                isGeneratingWallpaper = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isGeneratingWallpaper) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("生成中…")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("生成桌布")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val ok = saveWallpaperToGallery(context, wallpaperBitmap)
                                onShowMessage(if (ok) "已儲存桌布至相簿 Pictures/PixelAI-Studio" else "儲存失敗")
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("儲存")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    runCatching {
                                        WallpaperManager.getInstance(context).setBitmap(wallpaperBitmap)
                                    }.isSuccess
                                }
                                onShowMessage(if (ok) "已將莫奈桌布套用至主畫面" else "套用桌布失敗")
                            }
                        }
                    ) {
                        Text("設為桌布")
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text(
                        text = "Magic Editor 圖像分析橋接",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "支援從相簿選圖或系統「分享」傳入圖片，選擇風格模板即可執行修圖分析或一鍵送手機 Gemini 重繪。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stylePresets.forEach { preset ->
                        FilterChip(
                            selected = selectedStylePreset == preset,
                            onClick = { selectedStylePreset = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text(if (selectedImageUri == null) "選擇圖片" else "更換圖片")
                    }

                    Button(
                        enabled = selectedImageUri != null && hasApiKey && !isAnalyzingImage,
                        onClick = {
                            val uri = selectedImageUri ?: return@Button
                            scope.launch {
                                isAnalyzingImage = true
                                val jpeg = loadScaledJpeg(context, uri)
                                if (jpeg == null) {
                                    onShowMessage("無法讀取圖片內容")
                                    isAnalyzingImage = false
                                    return@launch
                                }
                                val prompt = """
                                    請扮演 Google Pixel Magic Editor 攝影與設計助理，針對「$selectedStylePreset」風格需求，用繁體中文分析這張照片：
                                    1. 【畫面重點與氛圍】
                                    2. 【針對「$selectedStylePreset」的修圖／構圖建議】
                                    3. 【社群貼文靈感】（2 句自然文案）
                                """.trimIndent()
                                imageAnalysis = when (val res = geminiClient.generateWithImage(prompt, jpeg, maxOutputTokens = 550)) {
                                    is GeminiResult.Success -> res.data
                                    is GeminiResult.Error -> "分析失敗：${res.message}"
                                }
                                isAnalyzingImage = false
                            }
                        }
                    ) {
                        if (isAnalyzingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("分析中…")
                        } else {
                            Text("AI 修圖分析")
                        }
                    }

                    OutlinedButton(
                        enabled = selectedImageUri != null,
                        onClick = {
                            val uri = selectedImageUri ?: return@OutlinedButton
                            val prompt = "請幫我以「$selectedStylePreset」風格處理或提供這張照片的 Magic Editor 修圖與構圖建議："
                            if (!GeminiAppHandoff.sendImageWithPrompt(context, uri, prompt)) {
                                onShowMessage("找不到可接收分享的 Gemini App")
                            }
                        }
                    ) {
                        Text("送給手機 Gemini")
                    }
                }

                imageAnalysis?.let { text ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun generatePaletteWithGemini(
    geminiClient: GeminiClient,
    theme: String
): Pair<List<Int>, String> {
    val prompt = """
        請為手機桌布主題「$theme」設計 3 個和諧的 HEX 色碼（從背景深色到亮點色）。
        嚴格用單行回覆，格式：#RRGGBB,#RRGGBB,#RRGGBB｜意境短評（15字內）
    """.trimIndent()
    return when (val res = geminiClient.generateText(prompt, maxOutputTokens = 80)) {
        is GeminiResult.Success -> {
            val hexMatches = Regex("#[0-9A-Fa-f]{6}").findAll(res.data).map {
                android.graphics.Color.parseColor(it.value)
            }.toList()
            val caption = res.data.substringAfter("｜", res.data).trim()
            if (hexMatches.size >= 3) {
                hexMatches.take(3) to caption
            } else {
                randomMonetPalette() to caption
            }
        }
        is GeminiResult.Error -> randomMonetPalette() to "已切換離線莫奈配色"
    }
}

private fun randomMonetPalette(): List<Int> {
    val presets = listOf(
        listOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt()),
        listOf(0xFF1A1B41.toInt(), 0xFF6290C3.toInt(), 0xFFC2E7DA.toInt()),
        listOf(0xFF3A1C71.toInt(), 0xFFD76D77.toInt(), 0xFFFFAF7B.toInt()),
        listOf(0xFF134E5E.toInt(), 0xFF71B280.toInt(), 0xFFD3E3FD.toInt())
    )
    return presets.random()
}

private fun renderPixelWallpaper(
    width: Int,
    height: Int,
    colors: List<Int>,
    seed: Int
): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors.toIntArray(),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val rng = Random(seed)
    val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    repeat(6) { idx ->
        shapePaint.color = colors[idx % colors.size]
        shapePaint.alpha = 45 + rng.nextInt(55)
        val cx = rng.nextFloat() * width
        val cy = rng.nextFloat() * height
        val radius = width * (0.22f + rng.nextFloat() * 0.35f)
        canvas.drawCircle(cx, cy, radius, shapePaint)
    }
    return bmp
}

private suspend fun saveWallpaperToGallery(context: Context, bitmap: Bitmap): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val filename = "PixelAI_Wallpaper_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/PixelAI-Studio"
                    )
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return@withContext false
            true
        } catch (_: Exception) {
            false
        }
    }

private suspend fun loadScaledJpeg(context: Context, uri: Uri, maxDim: Int = 1024): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val largest = max(bounds.outWidth, bounds.outHeight)
            val sample = if (largest > maxDim) (largest / maxDim).coerceAtLeast(1) else 1
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
            bmp.recycle()
            out.toByteArray()
        } catch (_: Exception) {
            null
        }
    }
