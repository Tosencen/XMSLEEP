# F-Droid 提交步骤指南

## 📋 准备工作检查

✅ 所有准备工作已完成：
- metadata.yml 文件已准备
- 截图已放置在正确位置
- 构建测试通过
- 代码已推送到 GitHub

## 🚀 提交步骤

### 步骤 1: Fork F-Droid 数据仓库

1. 访问 F-Droid 数据仓库：https://gitlab.com/fdroid/fdroiddata
2. 点击右上角的 **"Fork"** 按钮
3. 选择你的账号作为目标
4. 等待 Fork 完成

### 步骤 2: 克隆你的 Fork

打开终端，执行以下命令：

```bash
# 克隆你 Fork 的仓库
git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
cd fdroiddata
```

**注意**：将 `YOUR_USERNAME` 替换为你的 GitLab 用户名

### 步骤 3: 创建新分支

```bash
# 创建新分支
git checkout -b add-xmsleep
```

### 步骤 4: 创建元数据文件

```bash
# 创建应用元数据文件（文件名使用 applicationId）
touch metadata/org.xmsleep.app.yml
```

### 步骤 5: 复制 metadata.yml 内容

将项目中的 `metadata.yml` 文件内容复制到新创建的文件中：

```bash
# 方法1: 使用编辑器打开并复制内容
# 打开 metadata/org.xmsleep.app.yml
# 复制 /Users/chen/Desktop/XMSLEEP/metadata.yml 的内容

# 方法2: 直接复制（如果两个仓库在同一台机器上）
# cp /Users/chen/Desktop/XMSLEEP/metadata.yml metadata/org.xmsleep.app.yml
```

**重要**：文件内容在下面，你可以直接复制粘贴。

### 步骤 6: 提交更改

```bash
# 添加文件
git add metadata/org.xmsleep.app.yml

# 提交
git commit -m "Add XMSLEEP app"

# 推送到你的 Fork
git push origin add-xmsleep
```

### 步骤 7: 创建 Merge Request

1. 访问你的 GitLab Fork 页面：`https://gitlab.com/YOUR_USERNAME/fdroiddata`
2. 你会看到提示："The branch `add-xmsleep` was just pushed. Create merge request"
3. 点击 **"Create merge request"** 按钮
4. 填写 Merge Request 信息：
   - **Title**: `Add XMSLEEP app`
   - **Description**: 
     ```
     Add XMSLEEP - A white noise and natural sound player app
     
     - Application ID: org.xmsleep.app
     - License: MIT
     - Source: https://github.com/Tosencen/XMSLEEP
     - Version: 2.0.4 (versionCode: 24)
     ```
5. 点击 **"Create merge request"**

### 步骤 8: 等待审核

- F-Droid 审核团队会检查你的提交
- 审核可能需要几周到几个月的时间
- 关注 Merge Request 的评论和反馈
- 根据审核反馈进行必要的调整

## 📄 metadata.yml 文件内容

以下是需要复制到 `metadata/org.xmsleep.app.yml` 的内容：

```yaml
Categories:
  - Multimedia
License: MIT
AuthorName: Tosencen
AuthorEmail: 
AuthorWebSite: https://github.com/Tosencen
SourceCode: https://github.com/Tosencen/XMSLEEP
IssueTracker: https://github.com/Tosencen/XMSLEEP/issues
Changelog: https://github.com/Tosencen/XMSLEEP/releases
Donate: 
Bitcoin: 
Litecoin: 
FlattrID: 
LiberapayID: 
OpenCollective: 

AutoName: XMSLEEP
Summary: A white noise and natural sound player app to help you relax, focus, and sleep better.
Description: |-
  XMSLEEP is a professional white noise and natural sound playback app dedicated to providing you with high-quality audio experiences. The app includes a variety of carefully selected natural sounds, including rain, thunder, campfire, bird chirping, and more, to help you relax, improve focus, and enhance sleep quality.

  Built with Material Design 3 guidelines, the app features a clean and beautiful interface with smooth and intuitive operations. It supports multiple languages, customizable themes, independent volume control, quick play, favorites, and many other features to provide you with a personalized sound experience.

  Features:
  • Multiple white noise sounds: rain, thunder, campfire, cat purring, birds, crickets, and more
  • Online audio: support for dynamically loading more audio resources from GitHub
  • Seamless loop: audio supports seamless loop playback for an immersive experience
  • Volume control: support for independent volume adjustment for each sound, or one-click adjustment for all sounds
  • Beautiful animations: built-in sounds come with WebP animations to enhance visual experience
  • Material Design 3: adopts the latest Material Design 3 design guidelines
  • Theme switching: supports light/dark mode switching, adapts to system theme
  • Custom themes: multiple color themes available, supports dynamic colors
  • Countdown feature: set automatic stop playback time to help you control usage duration
  • Quick play area: support for adding frequently used sounds to the quick play area for quick access
  • Favorites: favorite your preferred white noise sounds
  • Global floating button: displays currently playing sounds, supports quick pause and expand to view
  • Auto update: supports automatic update checking via GitHub Releases

  Privacy:
  This app highly values your privacy protection. The app does not collect any personally identifiable information and does not track user activities.

RepoType: git
Repo: https://github.com/Tosencen/XMSLEEP.git

Builds:
  - versionName: '2.0.4'
    versionCode: 24
    commit: v2.0.4
    subdir: app
    gradle:
      - yes
    output: build/outputs/apk/release/app-release.apk

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: '2.0.4'
CurrentVersionCode: 24
```

## ⚠️ 注意事项

1. **文件名必须正确**：`metadata/org.xmsleep.app.yml`（使用 applicationId）
2. **Git 标签**：确保 GitHub 仓库中有 `v2.0.4` 标签
3. **构建路径**：已测试确认路径正确
4. **截图**：截图已通过 Fastlane 结构提供，F-Droid 会自动提取

## 🔗 相关链接

- F-Droid 数据仓库：https://gitlab.com/fdroid/fdroiddata
- 你的 GitLab 个人资料：https://gitlab.com/-/user_settings/profile
- XMSLEEP GitHub 仓库：https://github.com/Tosencen/XMSLEEP

## 📝 提交后

提交 Merge Request 后：
1. 保持关注 Merge Request 的更新
2. 及时响应审核人员的反馈
3. 如果需要修改，在分支上提交新的 commit 并 push
4. 审核通过后，应用会被添加到 F-Droid 应用库

祝提交顺利！🎉

