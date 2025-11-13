# Release 上传说明

## 📦 已生成的文件

### APK 文件
- **文件**: `~/Desktop/XMSLEEP-v2.0.4-release.apk`
- **大小**: 24 MB
- **版本**: 2.0.4 (versionCode: 24)

### 源码压缩包
- **文件**: `~/Desktop/XMSLEEP-2.0.4-source.zip`
- **大小**: 89 MB
- **内容**: 完整的源代码（不包含构建产物）

## 🚀 上传到 GitHub Releases

### 方法 1: 通过 GitHub 网页上传

1. **访问 Releases 页面**：
   - 访问：https://github.com/Tosencen/XMSLEEP/releases
   - 点击 "Releases" 或 "Tags"

2. **编辑现有 Release 或创建新 Release**：
   - 如果 v2.0.4 release 已存在，点击 "Edit release"
   - 如果不存在，点击 "Draft a new release"

3. **填写 Release 信息**：
   - **Tag**: `v2.0.4`
   - **Title**: `v2.0.4`
   - **Description**: 
     ```
     ## v2.0.4 (2025-11-12)
     
     ### 🔒 安全
     - 添加网络安全配置禁止 cleartext traffic
     - 禁用 DEPENDENCY_INFO_BLOCK（F-Droid 兼容性）
     
     ### 📦 优化
     - 优化音频文件为单声道、96kbps
     - 仅保留 arm64-v8a 架构以减小 APK 体积
     
     ### 📝 文档
     - 添加 Fastlane 元数据结构（F-Droid 需要）
     - 清理过时文档
     ```

4. **上传文件**：
   - 点击 "Attach binaries" 或拖拽文件
   - 上传 `XMSLEEP-v2.0.4-release.apk`
   - 上传 `XMSLEEP-2.0.4-source.zip`

5. **发布**：
   - 点击 "Publish release" 或 "Update release"

### 方法 2: 使用 GitHub CLI (如果已安装)

```bash
# 安装 GitHub CLI (如果未安装)
# brew install gh

# 登录
gh auth login

# 创建或更新 release
gh release create v2.0.4 \
  ~/Desktop/XMSLEEP-v2.0.4-release.apk \
  ~/Desktop/XMSLEEP-2.0.4-source.zip \
  --title "v2.0.4" \
  --notes "## v2.0.4 (2025-11-12)

### 🔒 安全
- 添加网络安全配置禁止 cleartext traffic
- 禁用 DEPENDENCY_INFO_BLOCK（F-Droid 兼容性）

### 📦 优化
- 优化音频文件为单声道、96kbps
- 仅保留 arm64-v8a 架构以减小 APK 体积

### 📝 文档
- 添加 Fastlane 元数据结构（F-Droid 需要）
- 清理过时文档"
```

## 📋 文件说明

### APK 文件
- 已签名的 Release APK
- 可直接安装到 Android 设备
- 适用于 arm64-v8a 架构

### 源码压缩包
- 包含完整的源代码
- 不包含构建产物（build/、.gradle/ 等）
- 基于当前 Git HEAD 创建
- 可用于源码审查或重新构建

## ✅ 检查清单

- [x] APK 已构建并复制到桌面
- [x] 源码压缩包已创建
- [ ] 上传 APK 到 GitHub Releases
- [ ] 上传源码压缩包到 GitHub Releases
- [ ] 更新 Release 说明






