# PixelAI-Bridge | Pixel 專屬功能仿生

> **免 Root（支援 Shizuku 增強）的 Google Pixel 專屬 AI 功能仿生與橋接工具，專為非 Pixel Android 裝置打造。**  
> **A rootless (Shizuku-enhanced) Pixel-exclusive AI features bionic & bridge suite for non-Pixel Android devices.**

---

## 🌟 專案概念 | Project Concept

許多 Google Pixel 專屬的實用 AI 功能（如 **Now Playing 聽歌識曲**、**Pixel Screenshots 截圖記憶庫**、**Magic Cue 語境建議**、**Pixel Studio 圖像生成**、**通話摘要** 等）受限於硬體與系統驗證而無法在一般 Android 手機（如 vivo、OPPO、小米、Samsung 等）上使用。

**PixelAI-Bridge** 旨在透過標準 Android API、**Gemini API**、裝置端 ML Kit 與選配的 **Shizuku** 特權橋接，在非 Pixel 裝置上還原並強化這些日常 AI 體驗。

Many Google Pixel exclusive AI features (**Now Playing**, **Pixel Screenshots**, **Magic Cue**, **Pixel Studio**, **Call Notes**, etc.) are locked to Pixel hardware/ROMs. **PixelAI-Bridge** recreates and bridges these experiences on non-Pixel Android devices (vivo, OPPO, Xiaomi, Samsung, etc.) using standard Android APIs, **Gemini API**, on-device ML Kit, and optional **Shizuku** privilege elevation.

---

## 🧩 核心功能模組規劃 | Planned Feature Modules

| 模組 | 中文名稱 | 英文名稱 | 技術實現路徑 |
| :--- | :--- | :--- | :--- |
| **A** | 聽歌識曲仿生 | Now Playing Bionic | `AudioRecord` / Quick Settings Tile / Shizuku 音訊擷取 + 音訊指紋與歷史紀錄 |
| **B** | AI 截圖記憶庫 | Pixel Screenshots Bionic | `MediaStore` 監聽 + ML Kit OCR + Gemini Vision 語意索引與問答 |
| **C** | 智慧語境建議 | Magic Cue & Context Bridge | `NotificationListener` / 懸浮氣泡 + 本地預篩 + Gemini 智慧回覆與行程/地圖捷徑 |
| **D** | AI 圖像工作室 | Pixel Studio & Magic Editor | Share Sheet 接入 + Gemini 圖像生成/編輯 API + 貼圖去背 |
| **E** | 語音轉錄與摘要 | Call Notes & Live Summary | 錄音檔 / 即時音訊串流 + Gemini 多語系逐字稿與重點摘要 |
| **F** | Shizuku 增強橋接 | Shizuku Privilege Bridge | 免 Root 授權 `WRITE_SECURE_SETTINGS`、省電白名單與系統級觸發 |

---

## 🛠️ 技術架構 | Tech Stack

- **語言與 UI / Language & UI**: Kotlin, Jetpack Compose, Material 3 (Dynamic Color / Monet)
- **架構模式 / Architecture**: Clean Architecture + MVVM + Multi-module (`:app`, `:core:ai`, `:core:shizuku`, `:core:data`, `:feature:*`)
- **依賴注入 / DI**: Hilt
- **本地儲存 / Local Storage**: Room Database + EncryptedSharedPreferences (API Key 安全儲存)
- **AI 引擎 / AI Engine**: Google Gemini API (BYOK) + ML Kit On-Device OCR
- **系統橋接 / System Bridge**: Shizuku API (`rikka.shizuku`)

---

## 📄 規劃文件 | Documentation

- [APK 規劃與架構設計書 (APK Design Plan)](docs/APK_DESIGN_PLAN.md)

---

## 📜 授權條款 | License

MIT License (TBD)
