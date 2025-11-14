# 安全配置说明 / Security Configuration

## 签名密钥配置 / Signing Key Configuration

### ⚠️ 重要安全提示 / Important Security Notes

1. **永远不要将 keystore 文件提交到 Git 仓库**  
   Never commit keystore files to Git repository

2. **不要在代码中硬编码密码**  
   Never hardcode passwords in source code

3. **使用 `gradle.properties` 存储敏感信息（已添加到 .gitignore）**  
   Use `gradle.properties` for sensitive data (already in .gitignore)

### 配置步骤 / Configuration Steps

#### 1. 生成签名密钥 / Generate Signing Key

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias xmsleep \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "YOUR_STORE_PASSWORD" \
  -keypass "YOUR_KEY_PASSWORD" \
  -dname "CN=XMSLEEP, OU=Development, O=XMSLEEP, L=Unknown, ST=Unknown, C=CN"
```

#### 2. 配置签名密码

**方法一：直接编辑 gradle.properties（推荐用于个人开发）**

在 `gradle.properties` 文件中填入密码（文件已在 .gitignore 中）：

```properties
RELEASE_STORE_PASSWORD=YOUR_STORE_PASSWORD
RELEASE_KEY_PASSWORD=YOUR_KEY_PASSWORD
```

**方法二：使用环境变量（推荐用于 CI/CD）**

设置环境变量：
```bash
export ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD=YOUR_PASSWORD
export ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD=YOUR_PASSWORD
```

**方法三：创建 gradle.properties.local（已废弃，仅供参考）**

创建 `gradle.properties.local` 文件存储密码（已在 .gitignore 中）

#### 3. 备份你的 keystore

将 `release.keystore` 和密码安全地备份到其他位置（不要提交到 Git）：
- 使用密码管理器存储密码
- 将 keystore 文件备份到安全的位置
- 如果丢失 keystore，将无法更新已发布的应用

### 文件说明 / File Description

- `gradle.properties` - 包含实际密码（已在 .gitignore 中）
- `gradle.properties.example` - 示例配置文件（可以提交）
- `release.keystore` - 发布签名密钥（已在 .gitignore 中）

## GitHub Token 配置

如需使用统计数据上传功能，在 `gradle.properties` 中设置：

```properties
GITHUB_TOKEN=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

创建 Token: https://github.com/settings/tokens  
需要权限: `gist` (创建、更新、读取 Gist)

---

## 当前状态 / Current Status

✅ 签名密码已从代码中移除  
✅ 使用 gradle.properties 管理敏感信息  
✅ .gitignore 已正确配置  
✅ 新的签名密钥已生成  

### 旧密钥处理

如果你之前使用的是硬编码密码 "xmsleep2025"：
- 旧的 keystore 已备份为 `release.keystore.old`
- 新的 keystore 使用相同密码但建议修改
- **强烈建议修改密码并妥善保管**
