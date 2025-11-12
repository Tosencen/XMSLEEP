# F-Droid 提交检查清单

## ✅ 已完成的项目

### 1. 代码和配置
- [x] 开源许可证（MIT License）
- [x] 代码混淆已禁用
- [x] 资源压缩已禁用
- [x] 依赖元数据已禁用
- [x] 无广告代码
- [x] 无追踪代码
- [x] 隐私政策已更新（移除统计信息）

### 2. 依赖库
- [x] 所有依赖库都是开源的
- [x] Coil: Apache License 2.0
- [x] Lottie: Apache License 2.0
- [x] MaterialKolor: Apache License 2.0

### 3. Git 和版本
- [x] Git 标签 v2.0.4 已创建
- [x] 版本号与标签匹配（versionCode: 24, versionName: 2.0.4）
- [x] 代码已推送到 GitHub

### 4. 文档和元数据
- [x] metadata.yml 模板文件已创建
- [x] F-DROID_SUBMISSION.md 指南已创建
- [x] Fastlane metadata 已存在

## ⚠️ 需要确认/补充的内容

### 1. metadata.yml 文件调整（提交到 F-Droid 时）

**文件位置和名称：**
- 在 F-Droid 的 `fdroiddata` 仓库中，文件名应该是：`metadata/org.xmsleep.app.yml`
- 文件名使用应用的 `applicationId`（`org.xmsleep.app`）

**需要检查的字段：**
- [ ] `commit`: 当前是 `v2.0.4`，确认标签存在 ✓
- [ ] `output`: `app/build/outputs/apk/release/app-release.apk` - 需要确认路径正确
- [ ] `AuthorEmail`: 当前为空，可选填写
- [ ] `subdir`: 当前是 `app`，正确 ✓

### 2. 应用图标（可选但推荐）

**当前状态：**
- 应用图标存在：`app/src/main/res/mipmap-*/ic_launcher.webp`
- 格式：WebP

**F-Droid 要求：**
- 如果需要，可以在 `metadata/org.xmsleep.app/icon.png` 提供 PNG 格式图标
- 建议尺寸：512x512 或更高
- 这不是必需的，F-Droid 可以从 APK 中提取图标

**操作建议：**
- 如果 F-Droid 审核时要求提供图标，再添加即可
- 可以先不准备，等审核反馈

### 3. 构建测试（推荐）

**建议在提交前测试：**
- [ ] 确认应用可以正常构建 Release APK
- [ ] 确认构建输出路径正确：`app/build/outputs/apk/release/app-release.apk`
- [ ] 测试 APK 可以正常安装和运行

**测试命令：**
```bash
./gradlew clean assembleRelease
```

### 4. metadata.yml 文件内容检查

**当前 metadata.yml 包含：**
- ✅ Categories: Multimedia
- ✅ License: MIT
- ✅ SourceCode: https://github.com/Tosencen/XMSLEEP
- ✅ IssueTracker: https://github.com/Tosencen/XMSLEEP/issues
- ✅ Changelog: https://github.com/Tosencen/XMSLEEP/releases
- ✅ AutoName: XMSLEEP
- ✅ Summary: 简短描述
- ✅ Description: 完整描述
- ✅ RepoType: git
- ✅ Repo: 仓库地址
- ✅ Builds: 构建配置
- ✅ AutoUpdateMode: Version v%v
- ✅ UpdateCheckMode: Tags
- ✅ CurrentVersion: 2.0.4
- ✅ CurrentVersionCode: 24

**可能需要调整：**
- ⚠️ `subdir: app` - 需要确认这是正确的（如果项目根目录就是构建目录，可能需要调整）
- ⚠️ `output` 路径 - 需要确认构建输出路径

### 5. 提交到 F-Droid 的步骤

**准备就绪后：**

1. **Fork fdroiddata 仓库**
   - 访问：https://gitlab.com/fdroid/fdroiddata
   - Fork 到你的账号

2. **克隆并创建分支**
   ```bash
   git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
   cd fdroiddata
   git checkout -b add-xmsleep
   ```

3. **创建元数据文件**
   - 创建文件：`metadata/org.xmsleep.app.yml`
   - 复制 `metadata.yml` 的内容到新文件
   - 根据实际情况调整内容

4. **提交并推送**
   ```bash
   git add metadata/org.xmsleep.app.yml
   git commit -m "Add XMSLEEP app"
   git push origin add-xmsleep
   ```

5. **创建 Merge Request**
   - 在 GitLab 网页上创建 MR
   - 等待审核

## 📝 注意事项

1. **subdir 字段**：
   - 如果项目根目录包含 `app` 子目录，`subdir: app` 是正确的
   - 如果项目根目录就是构建目录，可能需要移除或调整

2. **构建输出路径**：
   - 当前配置：`app/build/outputs/apk/release/app-release.apk`
   - 这是相对于 `subdir` 的路径
   - 如果 `subdir: app`，那么完整路径是：`app/app/build/outputs/apk/release/app-release.apk`
   - **需要确认实际构建输出路径！**

3. **Gradle 构建**：
   - `gradle: - yes` 表示使用默认构建
   - F-Droid 会自动运行 `./gradlew assembleRelease`

4. **作者邮箱**：
   - `AuthorEmail` 字段是可选的
   - 如果填写，F-Droid 可能会通过邮箱联系你

## 🎯 下一步行动

1. **立即可以做的：**
   - [ ] 测试本地构建：`./gradlew clean assembleRelease`
   - [ ] 确认 APK 输出路径
   - [ ] 如果需要，调整 `metadata.yml` 中的路径

2. **提交到 F-Droid 时：**
   - [ ] Fork fdroiddata 仓库
   - [ ] 创建 `metadata/org.xmsleep.app.yml` 文件
   - [ ] 根据实际构建路径调整配置
   - [ ] 提交 Merge Request

3. **等待审核时：**
   - [ ] 关注 GitLab MR 的评论
   - [ ] 根据审核反馈调整配置
   - [ ] 如果需要，提供应用图标

## ⚠️ 重要提醒

**最关键的是确认构建输出路径！**

当前配置中：
- `subdir: app` - 表示在 `app` 目录下构建
- `output: app/build/outputs/apk/release/app-release.apk` - 这是相对于 `subdir` 的路径

**实际路径应该是：**
- 如果 `subdir: app`，那么完整路径是：`app/app/build/outputs/apk/release/app-release.apk`
- 如果项目根目录就是构建目录，应该改为：`subdir: .` 或移除，`output: app/build/outputs/apk/release/app-release.apk`

**建议：**
1. 先测试构建，确认实际输出路径
2. 根据实际路径调整 `metadata.yml`
3. 然后再提交到 F-Droid

