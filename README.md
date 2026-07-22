<h1 align="center"> 📱 XMSLEEP
 </h1>

<div align="center">

一个专注于白噪音播放的 Android 应用，帮助你放松、专注和入眠。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://www.android.com/)

[下载应用](#下载) • [功能特性](#功能特性) • [使用说明](#使用说明)

**Language**: 中文 | [繁體中文](README_ZH_TW.md) | [English](README_EN.md) | [한국어](README_KO.md) | [Русский](README_RU.md) | [日本語](README_JA.md)

<a href="https://hellogithub.com/repository/Tosencen/XMSLEEP" target="_blank"><img src="https://abroad.hellogithub.com/v1/widgets/recommend.svg?rid=3cbf370c9f534ea3bf3695b7f9b8bd19&claim_uid=Gxvd2eIyHm54S9p" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>
</div>

## 📱 屏幕截图

<div align="center">

<table>
  <tr>
    <td align="center">
      <a href="screenshots/1.jpg"><img src="screenshots/1.jpg" alt="截图1" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/2.jpg"><img src="screenshots/2.jpg" alt="截图2" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/3.jpg"><img src="screenshots/3.jpg" alt="截图3" width="200"/></a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="screenshots/4.jpg"><img src="screenshots/4.jpg" alt="截图4" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/5.jpg"><img src="screenshots/5.jpg" alt="截图5" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/6.jpg"><img src="screenshots/6.jpg" alt="截图6" width="200"/></a>
    </td>
  </tr>
</table>

</div>

---

## 📱 关于

XMSLEEP 是一个专注于白噪音播放的 Android 应用，提供多种自然声音帮助您放松、专注和入眠。应用采用 Material Design 3 设计规范，界面简洁美观，操作流畅。

## ✨ 功能特性

### 🎵 音频功能
- **多种白噪音**：提供雨声、篝火、雷声、猫咪呼噜、鸟鸣、夜虫等多种自然声音
- **网络音频**：支持从 GitHub 动态加载更多音频资源
- **本地音频**：支持播放手机中的音频文件，顺序/随机/单曲循环模式
- **无缝循环**：音频支持无缝循环播放，提供沉浸式体验
- **音量控制**：支持单独调节每个声音的音量，或一键调整所有声音
- **音量持久化**：音量设置会在应用重启后自动保留
- **蓝牙耳机支持**：蓝牙耳机断开时自动暂停播放
- **B站电台**：支持搜索和播放 Bilibili 直播电台，可收藏常用房间
- **音频互斥**：本地音频与白噪音播放互斥，自动切换

### 🎨 界面与体验
- **精美动画**：内置声音配有 webp 动画，增强视觉体验
- **Material Design 3**：采用最新的 Material Design 3 设计规范
- **主题切换**：支持浅色/深色模式切换，适配系统主题
- **自定义主题**：多种颜色主题可选，支持动态颜色
- **自定义背景**：支持上传图片/视频作为应用背景，透明度/模糊度可调

### ⚙️ 实用功能
- **倒计时功能**：设置自动停止播放的时间，帮助您控制使用时长
- **预设播放区域**：支持 3 个预设，每个预设可保存最多 10 个常用声音，快速切换场景
- **收藏功能**：收藏喜欢的白噪音声音
- **最近播放**：应用重启时显示最近播放弹窗，快速恢复上次播放
- **全局浮动按钮**：显示正在播放的声音，支持快速暂停和展开查看
- **智能交互**：悬浮按钮与预设弹窗互斥，滚动页面时自动收起
- **自动更新**：支持通过 GitHub Releases 自动检查更新，下载状态持久化
- **简化版模式**：极简界面，3列瀑布流直接展示所有白噪音，无需分类浏览
- **天气智能推荐**：根据当前天气自动推荐合适的声音，支持 Lottie 动画显示

## 🛠️ 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化 UI 框架
- **Material Design 3** - UI 设计系统
- **ExoPlayer/Media3** - 音频播放引擎，支持无缝循环
- **OkHttp** - 网络请求和文件下载
- **Gson** - JSON 解析
- **Kotlinx Serialization** - JSON 序列化
- **Coil** - 图片加载
- **WebP** - 动画支持（声音卡片动画）
- **MaterialKolor** - 动态主题色生成
- **Accompanist** - Pull-to-refresh 支持
- **Hilt** - 依赖注入框架
- **Lottie** - 高质量动画渲染（天气图标动画）

## 📦 当前版本

- **版本号**: 2.2.9
- **Version Code**: 44
- **最低支持**: Android 8.0 (API 26)
- **目标版本**: Android 15 (API 35)

### 🆕 最新更新 (v2.2.9)

#### 🎨 新功能
- **简化版白噪音模式**：全新简化版界面，3列瀑布流直接展示所有白噪音，无需分类，一键进入沉浸式播放
- **简化版天气组件**：右上角实时显示天气（Lottie 动画 + 温度 + 描述），支持离线缓存
- **简化版倒计时**：右下角定时器按钮，支持播放时定时停止
- **简化版音量控制**：长按声音卡片可调整单个音量
- **简化版持久化**：退出简化版后再次打开 App 自动恢复简化版界面
- **简化版多语言**：简化版界面支持中文/英文/日文/韩文/俄文/繁体中文
- **天气 Lottie 动画**：设置页面天气图标替换为 Meteocons Lottie 动画（11种天气，白天/夜间自动切换）
- **天气缓存优化**：天气数据缓存延长至3小时，首次加载静默刷新，失败时保留旧缓存

#### ✨ 改进
- **设置页面重构**：标题行整合「简化版」入口按钮和天气设置按钮
- **简化版返回按钮**：使用 low_priority 图标，字号 18sp，更易点击
- **版本号升级**：2.2.8 → 2.2.9

### 历史版本

#### v2.2.8
- **背景系统重构**：全新全屏弹窗设计，支持内置背景、自定义图片/视频背景
- **背景预览**：新增手机预览 mock，实时展示背景效果
- **内置背景精简**：保留 bg_1 和 bg_6 两个精选背景
- **本地音频播放模式**：支持顺序播放、随机播放、单曲循环
- **音频互斥**：本地音频播放自动暂停白噪音，白噪音播放自动停止本地音频
- **进度保存/恢复**：本地音频播放位置自动保存，下次打开恢复上次进度
- **天气推荐预设按钮**：天气智能推荐页面右上角新增预设快捷入口

### 历史版本

#### v2.2.3
- **一言一句桌面小组件**：新增桌面小组件，显示时间、每日名言和刷新按钮

#### v2.2.1
- **呼吸练习功能**：新增呼吸引导功能，帮助用户进行放松练习
- **屏幕常亮功能**：优化屏幕常亮设置，支持保持屏幕常亮
- **天气音频映射**：优化天气与音频的映射关系，不同天气对应不同音效

## 🚀 下载

最新版本可在 [GitHub Releases](https://github.com/Tosencen/XMSLEEP/releases) 下载。

## 📋 构建要求

- **Android Studio**: Ladybug | 2024.2.1 或更高版本
- **JDK**: 17 或更高版本
- **Android SDK**: API 33 或更高版本
- **Gradle**: 8.0 或更高版本

## 🔨 构建步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/Tosencen/XMSLEEP.git
   cd XMSLEEP
   ```

2. **配置 Gradle**
   - 复制 `gradle.properties.example` 为 `gradle.properties`
   - （可选）配置 GitHub Token 以提升 API 限制

3. **打开项目**
   - 使用 Android Studio 打开项目
   - 同步 Gradle 依赖

4. **运行项目**
   - 连接设备或启动模拟器
   - 点击运行按钮

## 📖 使用说明

### 基本操作
1. **播放声音**：点击声音卡片开始播放，再次点击停止
2. **调整音量**：播放时点击卡片右下角的音量图标，可以单独调节每个声音的音量
3. **设置倒计时**：点击右下角的倒计时按钮，设置自动停止时间

### 界面操作
4. **切换主题**：点击左上角的主题切换按钮，在浅色和深色模式之间切换
5. **自定义设置**：在设置页面可以调整主题颜色、隐藏动画等
6. **预设管理**：点击声音卡片标题，选择"添加到预设"可将声音添加到当前预设
7. **预设切换**：在底部预设区域可以切换 3 个不同的预设，每个预设最多保存 10 个声音
8. **收藏功能**：点击声音卡片标题，选择"收藏"可将声音添加到收藏列表
9. **B站电台**：在电台页面点击搜索按钮，可按分类搜索 Bilibili 直播间，点击图钉图标收藏常用房间

### 高级功能
10. **全局浮动按钮**：当有声音播放时，会出现浮动按钮，点击可展开查看正在播放的声音
11. **批量添加到预设**：在浮动按钮展开状态下，可以将正在播放的声音批量添加到当前预设
12. **智能收起**：滚动页面或切换标签时，浮动按钮会自动收起，避免遮挡内容

## ⚠️ 声音来源说明

本应用中的声音来源如下：

- **内置声音**：来自开源音频资源库
- **网络声音**：来自 [moodist](https://github.com/remvze/moodist) 项目，遵循 MIT 开源许可协议
- **第三方资源**：部分声音来自第三方提供商，遵循相应的许可协议
  - 遵循 **Pixabay Content License** 的声音：[Pixabay Content License](https://pixabay.com/service/license-summary/)
  - 遵循 **CC0** 的声音：[Creative Commons Zero License](https://creativecommons.org/publicdomain/zero/1.0/)

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献指南
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 👤 作者

**Tosencen**

- GitHub: [@Tosencen](https://github.com/Tosencen)

## 🙏 致谢

- [moodist](https://github.com/remvze/moodist) - 网络音频资源来源
- [Material Design 3](https://m3.material.io/) - UI 设计规范
- [MaterialKolor](https://github.com/material-foundation/material-color-utilities) - 动态颜色方案

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star！**

© 2026 XMSLEEP. All rights reserved.

</div>
