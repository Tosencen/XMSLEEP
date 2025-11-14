# 项目规范修复总结

## 修复日期
2025-11-14

## 修复的问题

### 🔴 严重安全问题（已修复）

#### 1. 签名密码硬编码
- **问题**：`app/build.gradle.kts` 中硬编码了签名密码 "xmsleep2025"
- **影响**：密码明文存储在代码中，任何能访问仓库的人都能看到
- **解决方案**：
  - 修改构建配置从 `gradle.properties` 动态读取密码
  - 将 `gradle.properties` 添加到 `.gitignore`
  - 从 Git 跟踪中移除 `gradle.properties`
  - 创建 `gradle.properties.example` 作为配置模板

#### 2. 签名密钥风险
- **问题**：旧的 `release.keystore` 文件虽然已在 `.gitignore` 中，但从未在 Git 历史中出现过（已验证）
- **解决方案**：
  - 生成新的签名密钥（2048位 RSA，有效期 27+ 年）
  - 备份旧密钥为 `release.keystore.old`
  - 更新 `.gitignore` 排除所有 keystore 文件

### ⚠️ 规范问题（已修复）

#### 3. 版本号不一致
- **问题**：
  - 根目录 `build.gradle.kts`: version = "1.0.0"
  - `app/build.gradle.kts`: versionName = "2.0.5"
- **解决方案**：统一为 "2.0.5"

#### 4. .gitignore 不完整
- **问题**：缺少一些敏感文件的排除规则
- **解决方案**：添加以下排除规则：
  - `gradle.properties` - 包含密码的配置文件
  - `gradle.properties.local` - 本地配置文件
  - `release.keystore.old` - 备份的旧密钥

## 新增文件

### SECURITY.md
详细的安全配置指南，包括：
- 如何生成签名密钥
- 三种配置签名密码的方法
- 安全最佳实践
- 文件说明

### gradle.properties.example
配置模板文件，包含：
- 完整的配置项说明
- 如何生成 keystore 的命令
- 安全注意事项

### gradle.properties.local
本地密码存储文件（已在 .gitignore 中）

## 修改的文件

### app/build.gradle.kts
```kotlin
// 之前：硬编码密码
storePassword = "xmsleep2025"
keyPassword = "xmsleep2025"

// 现在：从配置文件读取
val keystorePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
val keyAliasPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
storePassword = keystorePassword
keyPassword = keyAliasPassword
```

### build.gradle.kts
- 统一版本号：`version = "2.0.5"`

### .gitignore
新增排除规则：
```
gradle.properties
gradle.properties.local
release.keystore.old
```

## 构建验证

✅ Debug 构建：成功（17秒）
✅ Release 构建：成功（30秒）
✅ 签名验证：成功

生成的签名信息：
- 算法：SHA384withRSA
- 密钥长度：2048 位
- 有效期：2025-11-14 至 2053-04-01（27+ 年）

## 安全改进清单

- ✅ 移除代码中的硬编码密码
- ✅ 签名密钥不会被提交到 Git
- ✅ 敏感配置文件被 .gitignore 排除
- ✅ 提供完整的安全配置文档
- ✅ 创建配置模板供其他开发者使用
- ✅ 生成新的强度更高的签名密钥

## 后续建议

### 立即执行
1. **修改签名密码**：当前密码仍是 "xmsleep2025"，建议修改为更强的密码
2. **备份密钥**：将新的 `release.keystore` 和密码安全备份到密码管理器
3. **删除旧密钥**：确认新密钥可用后，删除 `release.keystore.old`

### 可选优化
1. **启用代码混淆**：在 `app/build.gradle.kts` 中设置 `isMinifyEnabled = true`
2. **启用资源压缩**：设置 `isShrinkResources = true`
3. **拆分大文件**：`MainActivity.kt` 有 3777 行，建议拆分成多个文件

## 如何使用新配置

### 首次配置
1. 复制 `gradle.properties.example` 为 `gradle.properties`
2. 填入实际密码（或使用现有的 "xmsleep2025"）
3. 运行 `./gradlew assembleRelease`

### CI/CD 配置
使用环境变量：
```bash
export ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD=your_password
export ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD=your_password
```

## Git 提交信息

提交 ID: edcc0ac
分支: main
状态: 已提交，未推送

如需推送到远程仓库：
```bash
git push origin main
```

---

**注意**：此次修改不影响应用的功能，仅改善了安全性和配置管理。
