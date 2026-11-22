# PixelAI-Bridge 本地開發與交接紀錄 | Local Development Log

> 本文件用於記錄專案進度、環境設定注意事項、已驗證之建置結果與後續交接事項。

---

## 1. 專案基本資訊 | Project Info

- **專案名稱**：PixelAI-Bridge（Pixel 專屬功能仿生）
- **Application ID**：`com.dx9901mk5.pixelaibridge`
- **本地工作區**：`D:\Google雲端硬碟\Antigravity\Pixel專屬功能仿生`
- **GitHub 儲存庫**：https://github.com/dx9901mk5/PixelAI-Bridge (`main` 分支)
- **目前版本**：`0.1.0-alpha` (`versionCode = 1`)

---

## 2. 開發環境與建置備忘 | Environment & Build Notes

- **JDK**：須使用 **JDK 17**（本機路徑：`C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`）。
  - 注意：系統預設 `JAVA_HOME` 指向 Android Studio JBR（Java 25），直接執行 Gradle 8.11.1 會失敗，建置前須切換 `JAVA_HOME` 至 JDK 17。
- **Gradle / AGP**：Gradle `8.11.1` + AGP `8.7.3` + Kotlin `2.0.21`。
- **Android SDK**：`D:\Android\Sdk`（`compileSdk = 35`、`targetSdk = 35`、`minSdk = 26`）。
- **非 ASCII 路徑相容性**：因工作區路徑含中文（`Google雲端硬碟`、`Pixel專屬功能仿生`），已於 `gradle.properties` 設定 `android.overridePathCheck=true`。
- **編譯指令（PowerShell）**：
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
  .\gradlew.bat assembleDebug --no-daemon
  ```

---

## 3. 進度里程碑紀錄 | Milestone History

### 2026-11-22 — 專案初始化與 Phase 1 完成
- **Commit `88547fb`**：建立 GitHub 公開儲存庫 `PixelAI-Bridge`，新增 `.gitignore`（排除 `*.gdoc` 與建置產物）、中英文 `README.md` 與 `docs/APK_DESIGN_PLAN.md`。
- **Commit `b142c3e`**：完成 Phase 1 多模組專案骨架與基礎功能：
  - `:core:designsystem`：Material 3 Dynamic Color 主題與 Pixel 風格元件（`PixelSectionCard`、`StatusBadgePill`、`PixelFeatureRowCard`）。
  - `:core:ai`：`ApiKeyStore`（`EncryptedSharedPreferences` 加密儲存 BYOK 金鑰與模型選擇）及 `GeminiClient`（REST `generateContent` 連線測試與文字生成）。
  - `:core:shizuku`：`ShizukuBridge`（Binder 生命週期監聽、安裝/運行/授權狀態評估與一鍵請求權限）。
  - `:app`：三頁式導航（總覽 Dashboard、仿生模組列表、設定與橋接），支援 API Key 儲存、即時連線測試與 Shizuku 權限觸發。
- **APK 驗證**：`.\gradlew.bat assembleDebug --no-daemon` 建置成功（`BUILD SUCCESSFUL`），產出 `app/build/outputs/apk/debug/app-debug.apk`（18,246,815 bytes）。

---

## 4. 待辦事項 | Next Up

- **Phase 2（MVP）**：實作「AI 截圖記憶庫 (Pixel Screenshots Bionic)」，包含 `MediaStore` 新截圖偵測、ML Kit 裝置端 OCR、Room 本地索引與 Gemini Vision 語意搜尋／問答。
- **Phase 3**：聽歌識曲仿生（Now Playing）與智慧語境建議（Magic Cue）。
- **Phase 4**：AI 圖像工作室（Pixel Studio）與通話摘要（Call Notes）。
