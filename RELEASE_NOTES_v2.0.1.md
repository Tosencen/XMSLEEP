# XMSLEEP v2.0.1 发版说明

## 🎉 版本信息
- **版本号**: 2.0.1
- **Version Code**: 21
- **发布日期**: 2025年11月7日

## 🐛 修复

### 安装权限处理优化
- **修复安装权限处理逻辑**：解决了"无法启动安装程序"的问题
- **修复权限请求后的重试安装逻辑**：优化了用户授予权限后的安装流程
- **改进安装流程**：确保用户能够正确授予权限并完成安装
  - 当用户没有安装权限时，会引导用户前往系统设置授予权限
  - 用户授予权限后，可以点击"我已允许，继续安装"按钮完成安装
  - 修复了权限请求后无法正确获取待安装文件的问题

## 🔧 技术改进

### 代码规范优化
- **移除不必要的完整包名引用**：优化代码规范，使用导入的简写形式
  - 将 `kotlinx.coroutines.delay()` 改为 `delay()`
  - 将 `kotlinx.coroutines.CoroutineScope()` 改为 `CoroutineScope()`
  - 将 `kotlin.math.abs()` 改为 `abs()`
- **改进代码可读性**：统一代码风格，符合 Kotlin 编码规范

## 📦 文件变更

### 修改文件
- `app/src/main/kotlin/org/xmsleep/app/update/UpdateInstaller.kt`
  - 添加 `hasInstallPermission()` 方法检查安装权限
  - 添加 `requestInstallPermission()` 方法请求安装权限
  - 改进 `install()` 方法，增加文件存在性检查和更好的错误处理
  
- `app/src/main/kotlin/org/xmsleep/app/update/UpdateViewModel.kt`
  - 添加 `pendingInstallFile` 保存待安装的文件
  - 修改 `installApk()` 方法，先检查权限再安装
  - 修改 `retryInstall()` 方法，从保存的文件中获取待安装文件
  
- `app/src/main/kotlin/org/xmsleep/app/update/UpdateDialog.kt`
  - 添加 `InstallPermissionRequested` 状态的对话框
  - 优化权限请求流程的用户体验

- `app/src/main/kotlin/org/xmsleep/app/MainActivity.kt`
  - 优化代码规范，移除完整包名引用

- `app/src/main/kotlin/org/xmsleep/app/ui/FloatingPlayButton.kt`
  - 优化代码规范，移除完整包名引用

- `app/src/main/kotlin/org/xmsleep/app/ui/SoundsScreen.kt`
  - 优化代码规范，移除完整包名引用

- `app/src/main/kotlin/org/xmsleep/app/ui/FavoriteScreen.kt`
  - 优化代码规范，移除完整包名引用

- `app/src/main/kotlin/org/xmsleep/app/timer/TimerManager.kt`
  - 优化代码规范，移除完整包名引用

## 📝 使用说明

### 安装更新
1. 下载新版本 APK 文件
2. 如果系统提示需要安装权限，请前往系统设置授予权限
3. 返回应用，点击"我已允许，继续安装"按钮
4. 完成安装

## 🔄 从 v2.0.0 升级

如果您当前使用的是 v2.0.0 版本，建议升级到 v2.0.1 以获得更好的安装体验和代码质量改进。

## 📱 系统要求

- Android 8.0 (API 26) 或更高版本
- 建议 Android 12 (API 31) 或更高版本以获得最佳体验

## 🙏 致谢

感谢所有用户的反馈和支持！

