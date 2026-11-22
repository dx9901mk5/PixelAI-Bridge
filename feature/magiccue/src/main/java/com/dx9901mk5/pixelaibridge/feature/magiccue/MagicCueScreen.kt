package com.dx9901mk5.pixelaibridge.feature.magiccue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TipsAndUpdates
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dx9901mk5.pixelaibridge.core.ai.GeminiAppHandoff
import com.dx9901mk5.pixelaibridge.core.ai.GeminiClient
import com.dx9901mk5.pixelaibridge.core.ai.GeminiResult
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import com.dx9901mk5.pixelaibridge.core.designsystem.StatusBadgePill
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MagicCueScreen(
    padding: PaddingValues,
    geminiClient: GeminiClient,
    hasApiKey: Boolean,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notifications by MagicCueBus.notifications.collectAsStateWithLifecycle()

    var isListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var manualInput by remember { mutableStateOf("") }
    var cueResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    fun analyzeText(sourceText: String) {
        if (sourceText.isBlank()) return
        val localOtp = Regex("\\b\\d{4,8}\\b").find(sourceText)?.value
        if (!hasApiKey) {
            cueResult = buildString {
                append("【離線預篩結果】\n")
                if (localOtp != null) append("• 偵測到可能驗證碼/數字：$localOtp\n")
                append("• 建議：設定 Gemini API Key 可自動生成 3 則情境智慧回覆與行程/地圖提取。")
            }
            return
        }
        scope.launch {
            isAnalyzing = true
            val prompt = """
                你是 Google Pixel Magic Cue 智慧語境助理。請分析以下訊息或通知內容，用繁體中文精簡輸出：
                1. 【關鍵資訊提取】（時間/地點/驗證碼/訂單號，若無則寫無）
                2. 【建議行動】（例如加入行事曆、複製驗證碼、開啟地圖）
                3. 【3 則一鍵回覆草稿】（口吻自然、簡短）

                內容：
                $sourceText
            """.trimIndent()
            cueResult = when (val res = geminiClient.generateText(prompt, maxOutputTokens = 450)) {
                is GeminiResult.Success -> res.data
                is GeminiResult.Error -> "分析失敗：${res.message}"
            }
            isAnalyzing = false
        }
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
        item {
            PixelSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null)
                        Column {
                            Text(
                                text = "Magic Cue 通知語境橋接",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "已內建銀行／密碼 App 隱私排除保護",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    StatusBadgePill(
                        label = if (isListenerEnabled) "監聽中" else "未授權",
                        isPositive = isListenerEnabled
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("通知存取權設定")
                    }
                    OutlinedButton(
                        onClick = { isListenerEnabled = isNotificationListenerEnabled(context) }
                    ) {
                        Text("重新檢查")
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Text(
                    text = "即時語境分析台",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = { Text("貼上任何訊息、Email 或地址，立即產生 Magic Cue 建議…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                val quickActions = remember(manualInput) {
                    MagicCueActionExtractor.extract(manualInput)
                }
                if (quickActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickActions.forEach { action ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    MagicCueActionExtractor.execute(context, action, onShowMessage)
                                },
                                label = { Text(action.label) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !isAnalyzing && manualInput.isNotBlank(),
                        onClick = { analyzeText(manualInput) }
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("分析中…")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("生成智慧建議")
                        }
                    }
                    OutlinedButton(
                        enabled = manualInput.isNotBlank(),
                        onClick = {
                            val prompt = """
                                請協助分析以下訊息，提取關鍵資訊（時間/地點/驗證碼/訂單號）並提供 3 則自然的回覆草稿：

                                $manualInput
                            """.trimIndent()
                            if (!GeminiAppHandoff.sendText(context, prompt)) {
                                onShowMessage("找不到可接收分享的 Gemini App")
                            }
                        }
                    ) {
                        Text("送給手機 Gemini")
                    }
                }

                cueResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = result, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Magic Cue", result))
                                    onShowMessage("已複製建議內容")
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("複製結果")
                            }
                        }
                    }
                }
            }
        }

        if (notifications.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "近期擷取通知 (${notifications.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(onClick = { MagicCueBus.clearAll() }) {
                        Text("清空")
                    }
                }
            }
            items(notifications, key = { it.id }) { item ->
                ElevatedCard(
                    onClick = {
                        val combined = "${item.title}\n${item.text}".trim()
                        manualInput = combined
                        analyzeText(combined)
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title.ifBlank { item.packageName },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                MagicCueBus.addExcludedPackage(context, item.packageName)
                                onShowMessage("已將 ${item.packageName} 加入隱私黑名單")
                            }
                        ) {
                            Icon(Icons.Rounded.Block, contentDescription = "排除此 App")
                        }
                    }
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?: return false
    val cn = ComponentName(context, MagicCueNotificationListener::class.java).flattenToString()
    return flat.contains(cn)
}
