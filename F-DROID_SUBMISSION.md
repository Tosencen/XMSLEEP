# F-Droid 提交指南

本文档说明如何将 XMSLEEP 提交到 F-Droid 平台。

## 📋 准备工作检查清单

### ✅ 已完成的项目

1. **开源许可证** ✓
   - 项目使用 MIT License
   - 所有源代码已开源

2. **代码混淆和资源压缩** ✓
   - 已禁用代码混淆 (`isMinifyEnabled = false`)
   - 已禁用资源压缩 (`isShrinkResources = false`)
   - 已禁用依赖元数据 (`includeInApk = false`)

3. **隐私和追踪** ✓
   - 已移除所有统计数据收集功能
   - 应用不包含广告
   - 应用不追踪用户活动

4. **依赖库许可证** ✓
   - Coil: Apache License 2.0 (F-Droid 兼容)
   - Lottie: Apache License 2.0 (F-Droid 兼容)
   - MaterialKolor: Apache License 2.0 (F-Droid 兼容)
   - 其他依赖均为开源库

5. **应用描述** ✓
   - 已更新应用描述，移除统计数据相关内容
   - 已更新隐私说明

### 📝 需要确认的信息

1. **Git 标签**
   - 确保已创建版本标签：`v2.0.4`
   - 如果没有，需要创建：`git tag v2.0.4 && git push origin v2.0.4`

2. **应用图标**
   - F-Droid 需要应用图标文件
   - 图标路径：`app/src/main/res/mipmap-*/ic_launcher.webp`
   - 可能需要提供 PNG 格式的图标

3. **作者邮箱**（可选）
   - 如果需要，可以在 `metadata.yml` 中填写 `AuthorEmail`

## 📤 提交步骤

### 1. Fork F-Droid 数据仓库

1. 访问 [F-Droid 数据仓库](https://gitlab.com/fdroid/fdroiddata)
2. 在 GitLab 上注册/登录账号
3. Fork `fdroiddata` 仓库到你的账号

### 2. 克隆仓库并创建分支

```bash
git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
cd fdroiddata
git checkout -b add-xmsleep
```

### 3. 添加应用元数据

1. 在 `metadata` 目录下创建新文件：`metadata/org.xmsleep.app.yml`
2. 将项目根目录的 `metadata.yml` 文件内容复制到新文件中
3. **重要**：需要根据实际情况调整以下内容：
   - `commit`: 使用实际的 Git 标签或提交哈希
   - `output`: 确认 APK 输出路径是否正确
   - `AuthorEmail`: 如果需要，填写作者邮箱

### 4. 添加应用图标（如果需要）

如果 F-Droid 要求提供图标文件：
1. 将应用图标复制到 `metadata/org.xmsleep.app/` 目录
2. 文件名为 `icon.png` 或 `icon.jpg`

### 5. 提交 Merge Request

```bash
git add metadata/org.xmsleep.app.yml
git commit -m "Add XMSLEEP app"
git push origin add-xmsleep
```

然后在 GitLab 网页上创建 Merge Request。

## 📄 metadata.yml 文件说明

项目根目录的 `metadata.yml` 文件是模板文件，包含以下主要字段：

- **Categories**: 应用类别（Multimedia）
- **License**: 许可证类型（MIT）
- **SourceCode**: 源代码仓库 URL
- **IssueTracker**: 问题跟踪 URL
- **Changelog**: 更新日志 URL
- **AutoName**: 应用名称
- **Summary**: 简短描述（80字符以内）
- **Description**: 完整描述
- **Builds**: 构建配置
  - `versionName`: 版本名称
  - `versionCode`: 版本代码
  - `commit`: Git 标签或提交哈希
  - `subdir`: 构建子目录（app）
  - `gradle`: 构建任务（yes 表示使用默认构建）
  - `output`: APK 输出路径
- **AutoUpdateMode**: 自动更新模式
- **UpdateCheckMode**: 更新检查模式（Tags）
- **CurrentVersion**: 当前版本
- **CurrentVersionCode**: 当前版本代码

## ⚠️ 注意事项

1. **Git 标签**
   - 确保每个版本都有对应的 Git 标签
   - 标签格式：`v2.0.4`（与 versionName 对应）

2. **构建可重现性**
   - F-Droid 要求构建是可重现的
   - 确保构建脚本不依赖外部环境变量（除了必要的）

3. **版本更新**
   - 每次发布新版本时，需要更新 `metadata.yml` 中的版本信息
   - 在 F-Droid 数据仓库中添加新的构建条目

4. **审核时间**
   - F-Droid 审核可能需要几周到几个月的时间
   - 保持耐心，及时响应审核人员的反馈

## 🔗 相关链接

- [F-Droid 官方文档](https://f-droid.org/zh_Hans/docs/)
- [F-Droid 提交快速入门指南](https://f-droid.org/zh_Hans/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [F-Droid 构建元数据参考](https://f-droid.org/zh_Hans/docs/Build_Metadata_Reference/)
- [F-Droid 收录政策](https://f-droid.org/zh_Hans/docs/Inclusion_Policy/)

## 📝 检查清单

提交前请确认：

- [ ] Git 标签已创建并推送
- [ ] metadata.yml 文件已正确填写
- [ ] 应用描述已更新（移除统计数据相关内容）
- [ ] 所有依赖库都是开源的
- [ ] 代码中没有广告或追踪代码
- [ ] 构建脚本可以在 F-Droid 环境中运行
- [ ] 已测试本地构建是否成功

## 🎯 下一步

1. 检查并创建 Git 标签（如果还没有）
2. 根据实际情况调整 `metadata.yml` 文件
3. 按照上述步骤提交到 F-Droid
4. 等待审核并响应反馈

祝提交顺利！🎉

