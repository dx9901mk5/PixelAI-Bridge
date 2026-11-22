package com.dx9901mk5.pixelaibridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dx9901mk5.pixelaibridge.core.ai.ApiKeyStore
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelAiBridgeTheme
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelFeatureRowCard
import com.dx9901mk5.pixelaibridge.core.designsystem.PixelSectionCard
import com.dx9901mk5.pixelaibridge.core.designsystem.StatusBadgePill
import com.dx9901mk5.pixelaibridge.core.shizuku.ShizukuStatus
import com.dx9901mk5.pixelaibridge.feature.screenshots.ScreenshotRepository
import com.dx9901mk5.pixelaibridge.feature.screenshots.ScreenshotsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PixelAiApplication
        setContent {
            PixelAiBridgeTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(app.container))
                PixelAiBridgeApp(
                    vm = vm,
                    screenshotRepository = app.container.screenshotRepository
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PixelAiBridgeApp(
    vm: MainViewModel,
    screenshotRepository: ScreenshotRepository
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hasApiKey by vm.hasApiKey.collectAsStateWithLifecycle()
    val selectedModel by vm.selectedModel.collectAsStateWithLifecycle()
    val shizukuStatus by vm.shizukuStatus.collectAsStateWithLifecycle()
    val modules by vm.modules.collectAsStateWithLifecycle()
    val testStatus by vm.testStatus.collectAsStateWithLifecycle()
    val isTesting by vm.isTesting.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PixelAI Bridge",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pixel 專屬功能仿生 · Phase 2 MVP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                    label = { Text("總覽") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.ImageSearch, contentDescription = null) },
                    label = { Text("截圖記憶") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Widgets, contentDescription = null) },
                    label = { Text("仿生模組") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("設定") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> DashboardTab(
                padding = innerPadding,
                hasApiKey = hasApiKey,
                selectedModel = selectedModel,
                shizukuStatus = shizukuStatus,
                activeModuleCount = modules.count { it.enabled },
                onGoToSettings = { selectedTab = 3 },
                onGoToModules = { selectedTab = 1 },
                onRequestShizuku = vm::requestShizukuPermission
            )

            1 -> ScreenshotsScreen(
                padding = innerPadding,
                repository = screenshotRepository,
                hasApiKey = hasApiKey,
                onShowMessage = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )

            2 -> ModulesTab(
                padding = innerPadding,
                modules = modules,
                onToggle = vm::toggleModule,
                onModuleClick = { module ->
                    if (module.id == "screenshots") {
                        selectedTab = 1
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "${module.title} 將於 ${module.phaseLabel} 開放完整功能"
                            )
                        }
                    }
                }
            )

            3 -> SettingsTab(
                padding = innerPadding,
                initialApiKey = vm.getCurrentApiKey(),
                selectedModel = selectedModel,
                shizukuStatus = shizukuStatus,
                testStatus = testStatus,
                isTesting = isTesting,
                onSaveApiKey = vm::saveApiKey,
                onSelectModel = vm::selectModel,
                onTestConnection = vm::testGeminiConnection,
                onRefreshShizuku = vm::refreshShizuku,
                onRequestShizuku = vm::requestShizukuPermission
            )
        }
    }
}

@Composable
private fun DashboardTab(
    padding: PaddingValues,
    hasApiKey: Boolean,
    selectedModel: String,
    shizukuStatus: ShizukuStatus,
    activeModuleCount: Int,
    onGoToSettings: () -> Unit,
    onGoToModules: () -> Unit,
    onRequestShizuku: () -> Unit
) {
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
            PixelSectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "非 Pixel 裝置 AI 橋接核心已就緒",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "目前已啟用 $activeModuleCount / 5 個仿生模組。請先於設定頁綁定 Gemini API Key 與 Shizuku（選配）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onGoToModules) {
                        Text("探索仿生模組")
                    }
                    OutlinedButton(onClick = onGoToSettings) {
                        Text("設定金鑰")
                    }
                }
            }
        }

        item {
            PixelSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gemini AI 引擎",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "模型：$selectedModel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadgePill(
                        label = if (hasApiKey) "已設定金鑰" else "未設定金鑰",
                        isPositive = hasApiKey
                    )
                }
            }
        }

        item {
            PixelSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shizuku 特權橋接",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = shizukuDescription(shizukuStatus),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadgePill(
                        label = shizukuLabel(shizukuStatus),
                        isPositive = shizukuStatus == ShizukuStatus.READY
                    )
                }
                if (shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestShizuku) {
                        Text("立即授權 Shizuku")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModulesTab(
    padding: PaddingValues,
    modules: List<BionicModuleState>,
    onToggle: (String, Boolean) -> Unit,
    onModuleClick: (BionicModuleState) -> Unit
) {
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
        items(modules, key = { it.id }) { module ->
            PixelFeatureRowCard(
                title = module.title,
                subtitle = module.subtitle,
                badge = "${module.badge} · ${module.phaseLabel}",
                icon = iconForModule(module.id),
                enabled = module.enabled,
                onToggle = { onToggle(module.id, it) },
                onClick = { onModuleClick(module) }
            )
        }
    }
}

@Composable
private fun SettingsTab(
    padding: PaddingValues,
    initialApiKey: String,
    selectedModel: String,
    shizukuStatus: ShizukuStatus,
    testStatus: String?,
    isTesting: Boolean,
    onSaveApiKey: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onTestConnection: () -> Unit,
    onRefreshShizuku: () -> Unit,
    onRequestShizuku: () -> Unit
) {
    var keyInput by remember(initialApiKey) { mutableStateOf(initialApiKey) }

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
                    Icon(Icons.Rounded.Key, contentDescription = null)
                    Text(
                        text = "Gemini API (BYOK)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiKeyStore.SUPPORTED_MODELS.forEach { model ->
                        FilterChip(
                            selected = selectedModel == model,
                            onClick = { onSelectModel(model) },
                            label = { Text(model) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onSaveApiKey(keyInput) }) {
                        Text("儲存金鑰")
                    }
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !isTesting
                    ) {
                        Text(if (isTesting) "測試中…" else "測試連線")
                    }
                }
                if (testStatus != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            PixelSectionCard {
                Text(
                    text = "Shizuku 系統橋接",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = shizukuDescription(shizukuStatus),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRefreshShizuku) {
                        Text("重新偵測")
                    }
                    if (shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED) {
                        Button(onClick = onRequestShizuku) {
                            Text("請求授權")
                        }
                    }
                }
            }
        }
    }
}

private fun iconForModule(id: String): ImageVector = when (id) {
    "screenshots" -> Icons.Rounded.ImageSearch
    "now_playing" -> Icons.Rounded.GraphicEq
    "magic_cue" -> Icons.Rounded.TipsAndUpdates
    "pixel_studio" -> Icons.Rounded.Palette
    "call_notes" -> Icons.Rounded.Mic
    else -> Icons.Rounded.AutoAwesome
}

private fun shizukuLabel(status: ShizukuStatus): String = when (status) {
    ShizukuStatus.NOT_INSTALLED -> "未安裝"
    ShizukuStatus.NOT_RUNNING -> "未啟動"
    ShizukuStatus.PERMISSION_REQUIRED -> "待授權"
    ShizukuStatus.READY -> "已連線"
}

private fun shizukuDescription(status: ShizukuStatus): String = when (status) {
    ShizukuStatus.NOT_INSTALLED -> "裝置未安裝 Shizuku（仍可使用標準功能，安裝後可解鎖背景保活增強）"
    ShizukuStatus.NOT_RUNNING -> "已安裝 Shizuku，但服務尚未啟動"
    ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku 運行中，點擊授權即可啟用特權橋接"
    ShizukuStatus.READY -> "Shizuku 已授權並正常運作中"
}
