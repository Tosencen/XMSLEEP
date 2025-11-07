# XMSLEEP v2.0.0 发版说明

## 🎉 版本信息
- **版本号**: 2.0.0
- **Version Code**: 20
- **发布日期**: 2025年11月7日

## ✨ 新功能

### 🌟 全局浮动播放按钮
- 新增全局浮动播放按钮，当任何声音（本地或远程）播放时自动出现
- 按钮固定在屏幕左侧，距离底部 360dp
- 支持展开/收缩功能，展开后显示最多 3 个正在播放的卡片
- 按钮可长按拖动，拖动时底部显示红色停止播放区域
- 支持跨标签页显示，切换标签时自动收缩（如果已展开）
- 支持页面滚动时自动收缩（如果已展开）

### 🎯 长按拖动停止播放功能
- 长按浮动按钮可以自由拖动
- 拖动时底部显示红色停止播放区域（高度 120dp，按钮进入时变为 130dp）
- 将按钮拖动到红色区域并松手，会停止所有播放并跟随区域一起渐出消失
- 红色区域支持颜色变化（按钮进入时颜色变深）
- 支持渐入渐出动画

### 📝 关于弹窗更新
- 重新编写应用介绍，更详细地说明应用的功能和特性
- 新增声音来源说明部分，明确说明：
  - 内置声音来源：开源音频资源库
  - 网络声音来源：moodist 项目（https://github.com/remvze/moodist），遵循 MIT 开源许可协议
  - 版权声明和使用限制

## 🎨 UI/UX 改进
- 浮动按钮采用 Material Design 3 设计规范
- 红色停止区域支持主题适配（深色/浅色模式）
- 优化动画效果，使用渐入渐出动画替代滑动动画
- 改进颜色对比度，确保在深色和浅色主题下都有良好的可读性

## 🔧 技术改进
- 新增 `FloatingPlayButton` 组件，实现全局浮动播放按钮功能
- 新增 `CardComponents` 组件，优化卡片组件复用
- 新增导航系统，支持二级页面导航
- 优化状态管理，支持浮动按钮位置和展开状态的持久化
- 改进音频播放检测逻辑，支持本地和远程声音的统一管理

## 📦 文件变更
- 新增文件：
  - `app/src/main/kotlin/org/xmsleep/app/ui/FloatingPlayButton.kt`
  - `app/src/main/kotlin/org/xmsleep/app/ui/CardComponents.kt`
  - `app/src/main/kotlin/org/xmsleep/app/navigation/XMSleepNavigator.kt`
- 修改文件：
  - `app/build.gradle.kts` - 更新版本号到 2.0.0
  - `app/src/main/kotlin/org/xmsleep/app/MainActivity.kt` - 集成浮动按钮，更新关于弹窗
  - `app/src/main/res/values/strings.xml` - 更新应用说明和声音来源说明
  - `app/src/main/res/values-en/strings.xml` - 更新英文翻译
  - `app/src/main/res/values-zh-rTW/strings.xml` - 更新繁体中文翻译

## 🐛 Bug 修复
- 修复浮动按钮在展开状态下添加新卡片时自动收缩的问题
- 修复设置页面滚动时浮动按钮不收缩的问题
- 修复浮动按钮文字颜色在深色主题下的对比度问题

## 📱 使用说明

### 浮动播放按钮
1. 播放任何声音（本地或远程）时，浮动按钮会自动出现在屏幕左侧
2. 点击按钮区域可以展开/收缩，查看正在播放的卡片
3. 点击卡片可以暂停对应的声音
4. 长按按钮可以拖动，拖动时底部会显示红色停止区域
5. 将按钮拖动到红色区域并松手，会停止所有播放

### 关于应用
- 在设置页面点击"关于 XMSLEEP"可以查看应用信息
- 新增的声音来源说明详细说明了所有音频内容的来源和版权信息

## 🔗 相关链接
- GitHub 仓库: https://github.com/Tosencen/XMSLEEP
- 声音来源: https://github.com/remvze/moodist

## 🙏 致谢
感谢 moodist 项目提供的优质音频资源！

---

© 2025 XMSLEEP. All rights reserved.

