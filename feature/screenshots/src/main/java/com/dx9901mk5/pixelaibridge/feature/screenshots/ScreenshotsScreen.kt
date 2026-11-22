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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
    var records by remember { mutableStateOf<List<ScreenshotRecord>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgressText by remember { mutableStateOf<String?>(null) }
    var selectedRecord by remember { mutableStateOf<ScreenshotRecord?>(null) }
    var showAskDialog by remember { mutableStateOf(false) }
    var askInput by remember { mutableStateOf("") }
    var askAnswer by remember { mutableStateOf<String?>(null) }
    var isAsking by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        records = repository.search(searchQuery)
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (!hasPermission) {
            item {
                PixelSectionCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "授權讀取截圖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pixel Screenshots 仿生需要讀取系統相簿中的截圖，並於裝置端執行 ML Kit OCR 建立可搜尋索引。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(requiredPermission) }) {
                        Text("授予媒體讀取權限")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜尋截圖文字、分類或檔名…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(requiredPermission)
                            return@Button
                        }
                        if (isScanning) return@Button
                        scope.launch {
                            isScanning = true
                            scanProgressText = "正在搜尋新截圖…"
                            val summary = repository.scanNewScreenshots { done, total ->
                                scanProgressText = "正在索引 $done / $total…"
                            }
                            records = repository.search(searchQuery)
                            isScanning = false
                            scanProgressText = null
                            onShowMessage(
                                if (summary.scannedNew > 0) {
                                    "已新索引 ${summary.scannedNew} 張截圖（共 ${summary.totalIndexed} 張）"
                                } else {
                                    "沒有發現新的截圖（共 ${summary.totalIndexed} 張已索引）"
                                }
                            )
                        }
                    },
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(scanProgressText ?: "掃描中…")
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("掃描新截圖")
                    }
                }

                OutlinedButton(
                    onClick = { showAskDialog = true },
                    enabled = hasApiKey && records.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI 問答")
                }
            }
        }

        if (records.isEmpty()) {
            item {
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
                                text = "點擊上方「掃描新截圖」即可透過本機 ML Kit OCR 與 Gemini 建立記憶庫。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(records, key = { it.mediaId }) { record ->
                ScreenshotCard(
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
                        records = repository.search(searchQuery)
                        onShowMessage("已透過 Gemini 更新摘要")
                    } else {
                        onShowMessage("請先設定 Gemini API Key")
                    }
                }
            }
        )
    }

    if (showAskDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAsking) showAskDialog = false },
            title = { Text("詢問截圖記憶庫") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = askInput,
                        onValueChange = { askInput = it },
                        label = { Text("例如：我的取貨碼或 Wi-Fi 密碼是什麼？") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isAsking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini 正在檢索截圖紀錄…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    askAnswer?.let { answer ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isAsking && askInput.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isAsking = true
                            askAnswer = when (val res = repository.askAcrossScreenshots(askInput)) {
                                is GeminiResult.Success -> res.data
                                is GeminiResult.Error -> "查詢失敗：${res.message}"
                            }
                            isAsking = false
                        }
                    }
                ) {
                    Text("送出提問")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isAsking,
                    onClick = { showAskDialog = false }
                ) {
                    Text("關閉")
                }
            }
        )
    }
}

@Composable
private fun ScreenshotCard(
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
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp)),
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = record.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (record.ocrText.isNotBlank()) {
                    Text(
                        text = record.ocrText.replace('\n', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    onReanalyze: () -> Unit
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
                    Text("複製文字")
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
                context.contentResolver.loadThumbnail(uri, Size(240, 240), null)
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
    val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return fmt.format(Date(epochSec * 1000L))
}
