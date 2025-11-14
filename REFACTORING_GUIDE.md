# 代码重构指南 / Code Refactoring Guide

## 当前状态 Current Status

### 已完成 Completed
✅ 提取工具函数到 `utils/FileUtils.kt`  
✅ 提取主题形状到 `theme/Shapes.kt`  
✅ 提取深色模式枚举到 `theme/DarkModeOption.kt`

### 待优化 To Be Optimized

#### MainActivity.kt (3776 行 / lines)

这个文件包含了太多功能，建议逐步拆分成以下模块：

**优先级高 High Priority:**
1. **ui/settings/SettingsScreen.kt** (约 700 行)
   - 将 `SettingsScreen` Composable 移出
   - 包含设置页面的所有UI组件

2. **ui/settings/ThemeSettings.kt** (约 600 行)
   - `ThemeSettingsScreen`
   - `ThemePreviewPanel`
   - `DarkModeSelectPanel`
   - `ColorButton` 等主题相关组件

3. **ui/starsky/StarSkyScreen.kt** (约 800 行)
   - `StarSkyScreen` Composable
   - `RemoteSoundCard` 组件
   - 相关状态管理

**优先级中 Medium Priority:**
4. **ui/components/Dialogs.kt** (约 400 行)
   - `ClearCacheDialog`
   - `AboutDialog`
   - `LanguageSelectionDialog`
   - `VolumeAdjustDialog`

5. **ui/components/CommonComponents.kt** (约 300 行)
   - `SwitchItem`
   - `ThemeModeCard`
   - `ColorOption`
   - 其他通用UI组件

6. **theme/XMSLEEPTheme.kt** (约 200 行)
   - `XMSLEEPTheme` Composable
   - 主题配置逻辑
   - 颜色方案生成

**优先级低 Low Priority:**
7. **ui/MainScreen.kt** (约 500 行)
   - `MainScreen` Composable
   - 导航逻辑
   - Tab 切换

## 重构步骤 Refactoring Steps

### 1. 准备工作
```bash
# 创建目录结构（已完成）
mkdir -p app/src/main/kotlin/org/xmsleep/app/ui/{settings,starsky,components}
mkdir -p app/src/main/kotlin/org/xmsleep/app/{theme,utils}
```

### 2. 逐个文件提取

#### 示例：提取 SettingsScreen

```kotlin
// 1. 创建新文件 ui/settings/SettingsScreen.kt
package org.xmsleep.app.ui.settings

import androidx.compose.runtime.*
import org.xmsleep.app.R
// ... 其他必要的 imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    // ... 参数
) {
    // ... 实现
}

// 相关的辅助 Composable 函数
```

```kotlin
// 2. 在 MainActivity.kt 中添加 import
import org.xmsleep.app.ui.settings.SettingsScreen

// 3. 删除 MainActivity.kt 中的原实现
```

### 3. 测试
每次拆分后都要：
- ✅ 运行 `./gradlew assembleDebug` 确保编译通过
- ✅ 测试相关功能是否正常
- ✅ 提交 Git（如果测试通过）

### 4. 重复步骤 2-3
直到 MainActivity.kt 只剩下核心入口代码（约 200-300 行）

## 建议的文件大小上限

- **Activity**: ≤ 300 行
- **Screen Composable**: ≤ 500 行
- **普通 Composable**: ≤ 200 行
- **工具类**: ≤ 300 行

## 注意事项 Notes

### ⚠️ 重构风险
- MainActivity.kt 是应用的核心文件，修改可能引入 bug
- 建议在独立分支进行重构
- 每次只拆分一个文件，并充分测试

### ✅ 重构收益
- 更好的代码可维护性
- 更快的编译速度（增量编译更高效）
- 更容易的团队协作
- 更清晰的代码结构

### 🔧 IDE 工具
可以使用 Android Studio 的重构功能：
1. 选中要移动的函数/类
2. 右键 → Refactor → Move
3. 选择目标包/文件
4. 让 IDE 自动处理 imports

## 示例拆分计划 Sample Refactoring Plan

### 第一阶段（1-2小时）
- [ ] 提取 StarSkyScreen
- [ ] 提取 SettingsScreen（不包括子组件）
- [ ] 测试构建和基本功能

### 第二阶段（2-3小时）
- [ ] 提取 ThemeSettings 相关组件
- [ ] 提取对话框组件
- [ ] 测试主题切换和设置功能

### 第三阶段（1-2小时）
- [ ] 提取通用组件
- [ ] 提取 XMSLEEPTheme
- [ ] 最终测试和代码审查

### 第四阶段（1小时）
- [ ] 优化 imports
- [ ] 添加文档注释
- [ ] Git 提交

## 已提取的文件 Extracted Files

### utils/FileUtils.kt
包含文件操作相关的工具函数：
- `getDirectorySize()` - 计算目录大小
- `formatBytes()` - 格式化字节显示
- `calculateCacheSize()` - 计算缓存大小
- `clearApplicationCache()` - 清理缓存
- `deleteRecursive()` - 递归删除

### theme/Shapes.kt
包含自定义形状：
- `TopLeftDiagonalShape` - 左上对角线形状
- `BottomRightDiagonalShape` - 右下对角线形状

### theme/DarkModeOption.kt
深色模式选项枚举：
- `LIGHT` - 浅色模式
- `DARK` - 深色模式
- `AUTO` - 跟随系统

## 使用新提取的文件 Using Extracted Files

在 MainActivity.kt 顶部添加以下 imports（如果还没有）：

```kotlin
// 工具函数
import org.xmsleep.app.utils.*

// 主题相关
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.theme.TopLeftDiagonalShape
import org.xmsleep.app.theme.BottomRightDiagonalShape
```

然后可以删除 MainActivity.kt 中的重复定义（目前保留以保证兼容性）。

---

**提示**: 重构是一个渐进的过程，不需要一次性完成。可以在后续开发中逐步优化。
