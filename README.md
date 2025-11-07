 <h1 align="center"> 📱 XMSLEEP
  </h1>

<div align="center">

一个专注于白噪音播放的 Android 应用，帮助您放松、专注和入眠。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://www.android.com/)

[下载应用](#下载) • [功能特性](#功能特性) • [使用说明](#使用说明) • [更新日志](#更新日志)

</div>

---

## 📱 关于

XMSLEEP 是一个专注于白噪音播放的 Android 应用，提供多种自然声音帮助您放松、专注和入眠。应用采用 Material Design 3 设计规范，界面简洁美观，操作流畅。

## ✨ 功能特性

### 🎵 音频功能
- **多种白噪音**：提供雨声、篝火、雷声、猫咪呼噜、鸟鸣、夜虫等多种自然声音
- **网络音频**：支持从 GitHub 动态加载更多音频资源
- **无缝循环**：音频支持无缝循环播放，提供沉浸式体验
- **音量控制**：支持单独调节每个声音的音量，或一键调整所有声音

### 🎨 界面与体验
- **精美动画**：每个声音配有 Lottie 动画，增强视觉体验
- **Material Design 3**：采用最新的 Material Design 3 设计规范
- **主题切换**：支持浅色/深色模式切换，适配系统主题
- **自定义主题**：多种颜色主题可选，支持动态颜色

### ⚙️ 实用功能
- **倒计时功能**：设置自动停止播放的时间，帮助您控制使用时长
- **默认播放区域**：支持将常用声音添加到默认播放区域，快速访问
- **收藏功能**：收藏喜欢的白噪音声音
- **全局浮动按钮**：显示正在播放的声音，支持快速暂停和展开查看
- **自动更新**：支持通过 GitHub Releases 自动检查更新

## 🛠️ 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化 UI 框架
- **Material Design 3** - UI 设计系统
- **ExoPlayer/Media3** - 音频播放引擎，支持无缝循环
- **Lottie** - 动画支持
- **MaterialKolor** - 动态颜色方案
- **OkHttp** - 网络请求和文件下载
- **Kotlinx Serialization** - JSON 序列化

## 📦 当前版本

- **版本号**: 2.0.2
- **Version Code**: 22
- **最低支持**: Android 8.0 (API 26)
- **目标版本**: Android 15 (API 35)

## 🚀 下载

最新版本可在 [GitHub Releases](https://github.com/Tosencen/XMSLEEP/releases) 下载。

## 📋 构建要求

- **Android Studio**: Hedgehog | 2023.1.1 或更高版本
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
6. **默认播放区域**：点击声音卡片标题，选择"默认"可将声音添加到默认播放区域
7. **收藏功能**：点击声音卡片标题，选择"收藏"可将声音添加到收藏列表

### 高级功能
8. **全局浮动按钮**：当有声音播放时，会出现浮动按钮，点击可展开查看正在播放的声音
9. **长按拖动停止**：长按浮动按钮可拖动，拖动到底部红色区域可停止所有播放

## 📝 更新日志

### v2.0.2 (2025-11-07)
- ✨ **新功能**：从系统设置返回后自动检测安装权限并弹出安装对话框
- 🐛 **修复**：修复安装权限处理逻辑，解决"无法启动安装程序"的问题
- 🐛 **修复**：修复权限请求后的重试安装逻辑
- 🔧 **改进**：优化代码规范，移除不必要的完整包名引用

### v2.0.1 (2025-11-07)
- 🐛 **修复**：修复安装权限处理逻辑
- 🐛 **修复**：修复权限请求后的重试安装逻辑
- 🔧 **改进**：优化代码规范

### v2.0.0 (2025-11-07)
- ✨ **新功能**：全局浮动播放按钮，支持展开/收缩，显示最多 3 个正在播放的卡片
- ✨ **新功能**：长按拖动停止播放功能，拖动到红色区域停止所有播放
- ✨ **新功能**：关于弹窗更新，新增声音来源说明
- 🎨 **改进**：浮动按钮采用 Material Design 3 设计规范
- 🔧 **改进**：优化状态管理，支持浮动按钮位置和展开状态的持久化

### v1.0.4
- 优化更新检查逻辑
- 修复版本号读取问题
- 改进错误处理和日志输出
- 增强更新弹窗的用户体验

### v1.0.3
- 初始发布版本
- 支持多种白噪音播放
- 支持主题切换和自定义
- 支持倒计时和音量控制

查看完整更新日志：[GitHub Releases](https://github.com/Tosencen/XMSLEEP/releases)

## 📚 文档

更多技术文档请查看 [docs](./docs/) 目录：

- [音频架构设计](./docs/AUDIO_ARCHITECTURE.md) - 音频资源管理架构设计文档
- [音频资源管理](./docs/AUDIO_RESOURCES.md) - 音频资源管理说明
- [声音模块资源清单](./docs/SOUND_MODULES_RESOURCES.md) - 声音模块资源清单

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

**⭐ 如果这个项目对您有帮助，请给个 Star！**

© 2025 XMSLEEP. All rights reserved.

</div>
