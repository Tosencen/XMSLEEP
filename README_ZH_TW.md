<h1 align="center"> 📱 XMSLEEP
 </h1>

<div align="center">

一個專注於白噪音播放的 Android 應用，幫助你放鬆、專注和入眠。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://www.android.com/)

[下載應用](#下載) • [功能特性](#功能特性) • [使用說明](#使用說明)

**Language**: [中文](README.md) | 繁體中文 | [English](README_EN.md) | [한국어](README_KO.md) | [Русский](README_RU.md) | [日本語](README_JA.md)

<a href="https://hellogithub.com/repository/Tosencen/XMSLEEP" target="_blank"><img src="https://abroad.hellogithub.com/v1/widgets/recommend.svg?rid=3cbf370c9f534ea3bf3695b7f9b8bd19&claim_uid=Gxvd2eIyHm54S9p" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>
</div>

## 📱 螢幕截圖

<div align="center">

<table>
  <tr>
    <td align="center">
      <a href="screenshots/1.jpg"><img src="screenshots/1.jpg" alt="截圖1" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/2.jpg"><img src="screenshots/2.jpg" alt="截圖2" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/3.jpg"><img src="screenshots/3.jpg" alt="截圖3" width="200"/></a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="screenshots/4.jpg"><img src="screenshots/4.jpg" alt="截圖4" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/5.jpg"><img src="screenshots/5.jpg" alt="截圖5" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/6.jpg"><img src="screenshots/6.jpg" alt="截圖6" width="200"/></a>
    </td>
  </tr>
</table>

</div>

---

## 📱 關於

XMSLEEP 是一個專注於白噪音播放的 Android 應用，提供多種自然聲音幫助您放鬆、專注和入眠。應用採用 Material Design 3 設計規範，介面簡潔美觀，操作流暢。

## ✨ 功能特性

### 🎵 音訊功能
- **多種白噪音**：提供雨聲、篝火、雷聲、貓咪呼嚕、鳥鳴、夜蟲等多種自然聲音
- **網路音訊**：支援從 GitHub 動態載入更多音訊資源
- **本地音訊**：支援播放手機中的音訊檔案
- **無縫循環**：音訊支援無縫循環播放，提供沉浸式體驗
- **音量控制**：支援單獨調節每個聲音的音量，或一鍵調整所有聲音
- **音量持久化**：音量設定會在應用重啟後自動保留
- **藍牙耳機支援**：藍牙耳機斷開時自動暫停播放
- **B站電台**：支援搜尋和播放 Bilibili 直播電台，可收藏常用房間

### 🎨 介面與體驗
- **精美動畫**：內建聲音配有 webp 動畫，增強視覺體驗
- **Material Design 3**：採用最新的 Material Design 3 設計規範
- **主題切換**：支援淺色/深色模式切換，適配系統主題
- **自訂主題**：多種顏色主題可選，支援動態顏色

### ⚙️ 實用功能
- **倒數計時功能**：設定自動停止播放的時間，幫助您控制使用時長
- **預設播放區域**：支援 3 個預設，每個預設可儲存最多 10 個常用聲音，快速切換場景
- **收藏功能**：收藏喜歡的白噪音聲音
- **最近播放**：應用重啟時顯示最近播放彈窗，快速恢復上次播放
- **全域浮動按鈕**：顯示正在播放的聲音，支援快速暫停和展開檢視
- **智慧互動**：懸浮按鈕與預設彈窗互斥，滾動頁面時自動收起
- **自動更新**：支援透過 GitHub Releases 自動檢查更新，下載狀態持久化

## 🛠️ 技術棧

- **Kotlin** - 主要開發語言
- **Jetpack Compose** - 現代化 UI 框架
- **Material Design 3** - UI 設計系統
- **ExoPlayer/Media3** - 音訊播放引擎，支援無縫循環
- **OkHttp** - 網路請求和檔案下載
- **Gson** - JSON 解析
- **Kotlinx Serialization** - JSON 序列化
- **Coil** - 圖片載入
- **WebP** - 動畫支援（聲音卡片動畫）
- **MaterialKolor** - 動態主題色生成
- **Accompanist** - Pull-to-refresh 支援
- **Hilt** - 依賴注入框架
- **Lottie** - 高品質動畫渲染

## 📦 目前版本

- **版本號**: 2.2.6
- **Version Code**: 41
- **最低支援**: Android 8.0 (API 26)
- **目標版本**: Android 15 (API 35)

### 🆕 最新更新 (v2.2.6)

#### 🎨 新功能
- **B站電台**：新增 Bilibili 直播電台功能，支援搜尋自習室、白噪音等分類，一鍵播放直播間聲音
- **圖釘收藏**：支援收藏常用直播間，快速切換，非直播房間自動置灰提示

#### ✨ 改進
- **編輯模式優化**：退出彈窗時自動關閉圖釘編輯模式，避免誤操作
- **預設彈窗改進**：編輯預設名稱後關閉彈窗時自動重置編輯狀態

### 歷史版本

#### v2.2.3
- **一言一句桌面小工具**：新增桌面小工具，顯示時間、每日名言和重新整理按鈕

#### v2.2.1
- **呼吸練習功能**：新增呼吸引導功能，幫助用戶進行放鬆練習
- **螢幕常亮功能**：最佳化螢幕常亮設定，支援保持螢幕常亮
- **天氣音訊映射**：最佳化天氣與音訊的映射關係，不同天氣對應不同音效

## 🚀 下載

最新版本可在 [GitHub Releases](https://github.com/Tosencen/XMSLEEP/releases) 下載。

## 📋 建置要求

- **Android Studio**: Ladybug | 2024.2.1 或更高版本
- **JDK**: 17 或更高版本
- **Android SDK**: API 33 或更高版本
- **Gradle**: 8.0 或更高版本

## 🔨 建置步驟

1. **克隆倉庫**
   ```bash
   git clone https://github.com/Tosencen/XMSLEEP.git
   cd XMSLEEP
   ```

2. **配置 Gradle**
   - 複製 `gradle.properties.example` 為 `gradle.properties`
   - （可選）配置 GitHub Token 以提升 API 限制

3. **開啟專案**
   - 使用 Android Studio 開啟專案
   - 同步 Gradle 依賴

4. **執行專案**
   - 連線裝置或啟動模擬器
   - 點選執行按鈕

## 📖 使用說明

### 基本操作
1. **播放聲音**：點選聲音卡片開始播放，再次點選停止
2. **調整音量**：播放時點選卡片右下角的音量圖示，可以單獨調節每個聲音的音量
3. **設定倒數計時**：點選右下角的倒數計時按鈕，設定自動停止時間

### 介面操作
4. **切換主題**：點選左上角的主題切換按鈕，在淺色和深色模式之間切換
5. **自訂設定**：在設定頁面可以調整主題顏色、隱藏動畫等
6. **預設管理**：點選聲音卡片標題，選擇「新增到預設」可將聲音新增到目前預設
7. **預設切換**：在底部預設區域可以切換 3 個不同的預設，每個預設最多儲存 10 個聲音
8. **收藏功能**：點選聲音卡片標題，選擇「收藏」可將聲音新增到收藏列表
9. **B站電台**：在電台頁面點擊搜尋按鈕，可按分類搜尋 Bilibili 直播間，點擊圖釘圖示收藏常用房間

### 進階功能
10. **全域浮動按鈕**：當有聲音播放時，會出現浮動按鈕，點選可展開檢視正在播放的聲音
11. **批次新增到預設**：在浮動按鈕展開狀態下，可以將正在播放的聲音批次新增到目前預設
12. **智慧收起**：滾動頁面或切換標籤時，浮動按鈕會自動收起，避免遮擋內容

## ⚠️ 聲音來源說明

本應用中的聲音來源如下：

- **內建聲音**：來自開源音訊資源庫
- **網路聲音**：來自 [moodist](https://github.com/remvze/moodist) 專案，遵循 MIT 開源許可協議
- **第三方資源**：部分聲音來自第三方提供者，遵循相應的許可協議
  - 遵循 **Pixabay Content License** 的聲音：[Pixabay Content License](https://pixabay.com/service/license-summary/)
  - 遵循 **CC0** 的聲音：[Creative Commons Zero License](https://creativecommons.org/publicdomain/zero/1.0/)

## 📄 許可證

本專案採用 [MIT License](LICENSE) 許可證。

## 🤝 貢獻

歡迎提交 Issue 和 Pull Request！

### 貢獻指南
1. Fork 本倉庫
2. 建立特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 👤 作者

**Tosencen**

- GitHub: [@Tosencen](https://github.com/Tosencen)

## 🙏 致謝

- [moodist](https://github.com/remvze/moodist) - 網路音訊資源來源
- [Material Design 3](https://m3.material.io/) - UI 設計規範
- [MaterialKolor](https://github.com/material-foundation/material-color-utilities) - 動態顏色方案

---

<div align="center">

**⭐ 如果這個專案對你有幫助，請給個 Star！**

© 2026 XMSLEEP. All rights reserved.

</div>
