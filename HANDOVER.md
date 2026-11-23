# PixelAI-Bridge 專案交接手冊 | Project Handover Guide

> **最後更新**：2026-11-23  
> **GitHub 儲存庫**：https://github.com/dx9901mk5/PixelAI-Bridge (`main` 分支)  
> **最新 Release**：https://github.com/dx9901mk5/PixelAI-Bridge/releases/tag/v0.3.0  
> **目前版本**：`0.3.0` (`versionCode = 6`，五大模組 UI 改版 + 固定簽名 `keystore/pixelai-bridge.jks`)  
> **固定簽名 SHA-256**：`2bf2e008e535c13b1b87e260389a475fb9ec088c0d929d8b6e07d4d4ad2c5d39`

---

## 1. 快速接手檢查清單 | Quick Onboarding Checklist

- **Application ID**：`com.dx9901mk5.pixelaibridge`
- **SDK 規格**：`minSdk = 26` / `compileSdk = 35` / `targetSdk = 35`
- **建置工具**：Gradle `8.11.1` + AGP `8.7.3` + Kotlin `2.0.21` + Compose BOM `2024.12.01`
- **JDK 要求**：**必須使用 JDK 17**（本機：`C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`）。
  - 本機預設 `JAVA_HOME` 指向 Android Studio JBR（Java 25），會導致 Gradle 8.11.1 失敗，執行前務必覆寫 `$env:JAVA_HOME`。
- **編譯與產出路徑**：
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
  .\gradlew.bat assembleDebug --no-daemon
  ```
  - 因工作區位於 Google Drive（含中文路徑且有檔案鎖定），`build.gradle.kts` 已將各模組 `layout.buildDirectory` 重導向至 `%TEMP%\pixelai-bridge-build\`，並於 `gradle.properties` 設定 `android.overridePathCheck=true`。
  - 已驗證可安裝 APK：`PixelAI-Bridge-debug.apk`（位於工作區根目錄，已列入 `.gitignore`）。

---

## 2. 模組架構與關鍵檔案索引 | Module & File Map

| 模組 | 職責 | 關鍵類別與檔案 |
| :--- | :--- | :--- |
| `:app` | 主程式入口、5 分頁導航、權限中心、自動更新 | `PixelAiApplication.kt`、`MainViewModel.kt`、`MainActivity.kt`、`PermissionCenter.kt`、`GitHubUpdater.kt`、`AutoUpdateWorker.kt`、`AndroidManifest.xml` |
| `:core:designsystem` | Material 3 Dynamic Color 主題與元件 | `Theme.kt`、`PixelComponents.kt` |
| `:core:ai` | Gemini REST Client (`gemini-3.8-flash` / `4-argon` / `3.5-flash-lite`) 與免 Key 橋接 | `ApiKeyStore.kt`、`GeminiClient.kt`、`GeminiAppHandoff.kt` |
| `:core:shizuku` | Shizuku Binder 狀態監測與權限請求 | `ShizukuBridge.kt` |
| `:feature:screenshots` | AI 截圖記憶庫（2 欄瀑布流 + Collections + ML Kit OCR + 問截圖） | `ScreenshotIndexStore.kt`、`ScreenshotOcrEngine.kt`、`ScreenshotRepository.kt`、`ScreenshotsScreen.kt` |
| `:feature:nowplaying` | 聽歌識曲仿生（快速設定磁貼 + 6 秒 WAV 取樣 + 最愛收藏 + 通知） | `NowPlayingStore.kt`、`NowPlayingRecognizer.kt`、`NowPlayingTileService.kt`、`NowPlayingScreen.kt` |
| `:feature:magiccue` | 智慧語境建議（通知監聽 + 隱私黑名單 + 一鍵動作晶片） | `MagicCueBus.kt`、`MagicCueNotificationListener.kt`、`MagicCueActionExtractor.kt`、`MagicCueScreen.kt` |
| `:feature:studio` | AI 圖像工作室（莫奈幾何桌布生成 + 修圖分析） | `PixelStudioScreen.kt` |
| `:feature:callnotes` | 通話與會議摘要（即時錄音 / 匯入音訊 / 文字整理） | `CallNotesStore.kt`、`CallNotesScreen.kt` |

---

## 3. 本機儲存與權限清單 | Local Storage & Permissions

- **SQLite 資料庫**：
  - `pixelai_screenshots.db`（截圖 OCR 與 AI 摘要索引）
  - `pixelai_nowplaying.db`（識曲歷史紀錄）
  - `pixelai_callnotes.db`（通話／會議摘要紀錄）
- **SharedPreferences**：
  - `pixelai_secure_prefs`（`EncryptedSharedPreferences`，儲存 Gemini API Key 與模型選擇；Keystore 異常裝置自動降級 `pixelai_prefs_fallback`）
  - `pixelai_magiccue_prefs`（Magic Cue 排除之 App 套件名稱黑名單）
- **AndroidManifest 權限與元件**：
  - `INTERNET`、`POST_NOTIFICATIONS`、`RECORD_AUDIO`、`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE (<=32)`
  - `ShizukuProvider` (`rikka.shizuku.ShizukuProvider`)
  - `MagicCueNotificationListener` (`BIND_NOTIFICATION_LISTENER_SERVICE`)
  - `MainActivity` 支援 `ACTION_SEND` (`image/*`) 直接分享圖片進入 Pixel Studio

---

## 4. 已知限制與後續交接建議 | Known Items & Next Steps

1. **Kotlin 編譯警告**：`NowPlayingScreen.kt` 中 `Icons.Rounded.OpenInNew` 建議改為 `Icons.AutoMirrored.Rounded.OpenInNew`。
2. **進階系統整合（待實作）**：
   - 新增 `TileService` 讓使用者可從快速設定面板一鍵觸發 **Now Playing 識曲**。
   - 新增 **WorkManager** 背景自動監聽新截圖，無須手動點擊「掃描新截圖」。
   - 針對 **vivo OriginOS / OPPO ColorOS / 小米 HyperOS** 實作 Shizuku 輔助指令（如背景保活、通知監聽權限引導）。
   - 為 **Magic Cue** 加上選配的懸浮膠囊氣泡（`SYSTEM_ALERT_WINDOW`）。
3. **相關文件**：
   - [專案架構與設計規劃書](docs/APK_DESIGN_PLAN.md)
   - [開發里程碑紀錄](docs/DEV_LOG.md)

---

## 5. v0.3.0 示範影片對照與改版規格 | Benchmark & v0.3.0 Redesign Spec

以下為對照 Google Pixel 官方示範影片後整理出的下一階段（`v0.3.0`）優先改版方向：

| 模組 | 參考示範影片（含時間軸） | v0.3.0 改版實作重點 |
| :--- | :--- | :--- |
| **Pixel Screenshots** | [現場示範 01:22](https://www.youtube.com/watch?v=Zm0bdWk4ROU&t=82s) | 1. 改為 **2 欄瀑布流大圖網格** + 頂部 Collections 分類膠囊。<br>2. 搜尋列直接整合「問截圖」模式。<br>3. 支援背景自動偵測新截圖。 |
| **Now Playing** | [設定與實測 00:00](https://www.youtube.com/watch?v=xVhBSsc5HtE&t=0s) | 1. 新增 **Quick Settings TileService** 一鍵識曲。<br>2. 辨識結果推播常駐音樂通知膠囊。<br>3. 歷史清單加入「愛心收藏」與每日分組。 |
| **Magic Cue** | [MadeByGoogle '25 00:00](https://www.youtube.com/watch?v=v0nxfQ2_n0c&t=0s) | 1. 實作 `SYSTEM_ALERT_WINDOW` 邊緣懸浮快捷膠囊。<br>2. 加入一鍵動作晶片（地址→Google Maps、時間→行事曆、驗證碼→複製）。 |
| **Pixel Studio & Magic Editor** | [Pixel Studio 03:03](https://www.youtube.com/watch?v=Zm0bdWk4ROU&t=183s)<br>[Magic Editor 00:40](https://www.youtube.com/watch?v=Dk3zK5EtCHE&t=40s) | 1. 串接 `gemini-nano-banana-2.1` / `gemini-3.1-flash-image` 直接生成圖片與貼圖（有 Key 時）。<br>2. 無 Key 時提供預設風格模板一鍵送手機 Gemini App。 |
| **Call Notes** | [Call Notes 00:00](https://www.youtube.com/watch?v=Zm0bdWk4ROU&t=0s) | 1. 自動掃描 vivo/OPPO/小米系統通話錄音資料夾並推播一鍵摘要通知。<br>2. 摘要頁改為「重點條列 + 可折疊逐字稿」排版。 |
