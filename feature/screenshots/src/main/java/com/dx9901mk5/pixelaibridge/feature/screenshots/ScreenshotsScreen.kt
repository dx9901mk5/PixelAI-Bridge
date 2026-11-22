package com.dx9901mk5.pixelaibridge.feature.screenshots

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dx9901mk5.pixelaibridge.core.ai.GeminiAppHandoff
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenshotsScreen(
    padding: PaddingValues,
    repository: ScreenshotRepository,
    hasApiKey: Boolean,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val requiredPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            onShowMessage("需要媒體讀取權限才能掃描手機截圖")
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isAskMode by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf("全部") }
    var allRecords by remember { mutableStateOf<List<ScreenshotRecord>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgressText by remember { mutableStateOf<String?>(null) }
    var selectedRecord by remember { mutableStateOf<ScreenshotRecord?>(null) }
    var inlineAskAnswer by remember { mutableStateOf<String?>(null) }
    var isAsking by remember { mutableStateOf(false) }
    var didAutoScan by remember { mutableStateOf(false) }

    suspend fun runScan(silentIfEmpty: Boolean = false) {
        if (isScanning) return
        isScanning = true
        scanProgressText = "正在搜尋新截圖…"
        val summary = repository.scanNewScreenshots { done, total ->
            scanProgressText = "索引中 $done / $total…"
        }
        allRecords = repository.search(if (isAskMode) "" else searchQuery)
        isScanning = false
        scanProgressText = null
        if (!silentIfEmpty || summary.scannedNew > 0) {
            onShowMessage(
                if (summary.scannedNew > 0) {
                    "已新索引 ${summary.scannedNew} 張截圖（共 ${summary.totalIndexed} 張）"
                } else {
                    "目前共 ${summary.totalIndexed} 張截圖已索引"
                }
            )
        }
    }

    LaunchedEffect(searchQuery, isAskMode) {
        allRecords = repository.search(if (isAskMode) "" else searchQuery)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && !didAutoScan) {
            didAutoScan = true
            if (repository.count() == 0) {
                runScan(silentIfEmpty = true)
            }
        }
    }

    val collections = remember(allRecords) {
        val counts = allRecords.groupingBy { it.category }.eachCount()
        listOf("全部" to allRecords.size) + counts.entries.sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    val filteredRecords = remember(allRecords, selectedCollection) {
        if (selectedCollection == "全部") {
            allRecords
        } else {
            allRecords.filter { it.category == selectedCollection }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 10.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (!hasPermission) {
            item(span = { GridItemSpan(2) }) {
                PixelSectionCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "授權讀取截圖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pixel Screenshots 仿生需要讀取系統相簿截圖，並於本機執行 ML Kit OCR 建立可搜尋記憶庫。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { permissionLauncher.launch(requiredPermission) }) {
                        Text("授予媒體讀取權限")
                    }
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isAskMode,
                        onClick = {
                            isAskMode = false
                            inlineAskAnswer = null
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("搜尋截圖") }
                    )
                    FilterChip(
                        selected = isAskMode,
                        onClick = { isAskMode = true },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("問截圖") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        enabled = !isScanning,
                        onClick = {
                            if (!hasPermission) {
                                permissionLauncher.launch(requiredPermission)
                            } else {
                                scope.launch { runScan(silentIfEmpty = false) }
                            }
                        }
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(scanProgressText ?: "掃描中", maxLines = 1)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("掃描")
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(if (isAskMode) "問截圖：例如「Wi-Fi 密碼或取貨碼？」" else "搜尋截圖文字、分類或檔名…")
                    },
                    leadingIcon = {
                        Icon(
                            if (isAskMode) Icons.Rounded.AutoAwesome else Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (isAskMode && searchQuery.isNotBlank()) {
                            IconButton(
                                enabled = !isAsking && allRecords.isNotEmpty(),
                                onClick = {
                                    if (!hasApiKey) {
                                        val snippetContext = allRecords.take(12).joinToString("\n") { rec ->
                                            "- [${rec.displayName} | ${rec.category}] ${rec.ocrText.replace('\n', ' ').take(180)}"
                                        }
                                        val prompt = "請根據我手機截圖的 OCR 紀錄回答問題：「$searchQuery」\n\n截圖紀錄：\n$snippetContext"
                                        if (!GeminiAppHandoff.sendText(context, prompt)) {
                                            onShowMessage("找不到可接收分享的 Gemini App")
                                        }
                                        return@IconButton
                                    }
                                    scope.launch {
                                        isAsking = true
                                        inlineAskAnswer = when (val res = repository.askAcrossScreenshots(searchQuery)) {
                                            is GeminiResult.Success -> res.data
                                            is GeminiResult.Error -> "查詢失敗：${res.message}"
                                        }
                                        isAsking = false
                                    }
                                }
                            ) {
                                if (isAsking) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "送出")
                                }
                            }
                        } else if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        inlineAskAnswer?.let { answer ->
            item(span = { GridItemSpan(2) }) {
                PixelSectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "截圖記憶庫 AI 回答",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { inlineAskAnswer = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "關閉", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = answer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (allRecords.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(collections, key = { it.first }) { (name, count) ->
                        FilterChip(
                            selected = selectedCollection == name,
                            onClick = { selectedCollection = name },
                            label = { Text("$name ($count)") }
                        )
                    }
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                PixelSectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ImageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "尚無符合的截圖紀錄",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "點擊右上「掃描」即可透過本機 ML Kit OCR 建立截圖瀑布流記憶庫。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.mediaId }) { record ->
                ScreenshotGridCard(
                    record = record,
                    onClick = { selectedRecord = record }
                )
            }
        }
    }

    selectedRecord?.let { rec ->
        ScreenshotDetailDialog(
            record = rec,
            hasApiKey = hasApiKey,
            onDismiss = { selectedRecord = null },
            onCopyOcr = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Screenshot OCR", rec.ocrText))
                onShowMessage("已複製 OCR 文字")
            },
            onReanalyze = {
                scope.launch {
                    val updated = repository.reanalyzeWithGemini(rec)
                    if (updated != null) {
                        selectedRecord = updated
                        allRecords = repository.search(if (isAskMode) "" else searchQuery)
                        onShowMessage("已透過 Gemini 更新摘要")
                    } else {
                        onShowMessage("請先設定 Gemini API Key")
                    }
                }
            },
            onSendToGeminiApp = {
                val prompt = buildString {
                    append("請幫我分析這張截圖的重點與待辦資訊：\n")
                    if (rec.ocrText.isNotBlank()) {
                        append("OCR 文字：\n${rec.ocrText.take(1200)}")
                    }
                }
                val ok = GeminiAppHandoff.sendImageWithPrompt(context, Uri.parse(rec.contentUri), prompt)
                if (!ok) onShowMessage("找不到可接收分享的 Gemini App")
            }
        )
    }
}

@Composable
private fun ScreenshotGridCard(
    record: ScreenshotRecord,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = record.contentUri) {
        value = loadThumbnailBitmap(context, Uri.parse(record.contentUri))
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = record.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ImageSearch, contentDescription = null)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = record.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatEpochSeconds(record.dateAddedSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = record.aiSummary.ifBlank { record.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScreenshotDetailDialog(
    record: ScreenshotRecord,
    hasApiKey: Boolean,
    onDismiss: () -> Unit,
    onCopyOcr: () -> Unit,
    onReanalyze: () -> Unit,
    onSendToGeminiApp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "分類：${record.category} · ${formatEpochSeconds(record.dateAddedSec)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.aiSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.ocrText.ifBlank { "（此截圖無可辨識文字）" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSendToGeminiApp) {
                    Text("送給手機 Gemini")
                }
                if (hasApiKey) {
                    OutlinedButton(onClick = onReanalyze) {
                        Text("AI 重析")
                    }
                }
                Button(
                    onClick = onCopyOcr,
                    enabled = record.ocrText.isNotBlank()
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("複製")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        }
    )
}

private suspend fun loadThumbnailBitmap(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(360, 360), null)
            } else {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

private fun formatEpochSeconds(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
    return fmt.format(Date(epochSec * 1000L))
}
