# XMSLEEP 统计数据监控使用说明

## 功能说明

本功能允许你通过 GitHub Gist 监控应用的使用情况，包括：
- 应用启动次数
- 总使用时长
- 活跃天数
- 活跃设备数
- GitHub Releases 下载量

## 配置步骤

### 1. 创建 GitHub Personal Access Token

1. 访问 GitHub: https://github.com/settings/tokens
2. 点击 "Generate new token" → "Generate new token (classic)"
3. 设置 Token 名称（如：XMSLEEP Stats）
4. 选择权限：勾选 `gist` 权限（创建、更新、读取 Gist）
5. 点击 "Generate token"
6. **重要**：复制生成的 Token（格式：`ghp_xxxxxxxxxxxxx`），只显示一次

### 2. 配置应用

1. 在项目根目录找到 `gradle.properties` 文件（如果没有，复制 `gradle.properties.example`）
2. 添加以下配置：
   ```properties
   GITHUB_TOKEN=ghp_你的token
   ```
3. 重新构建应用

### 3. 使用应用

应用会自动：
- 每次启动时记录使用数据
- 每 24 小时自动上传一次统计数据到私有 GitHub Gist
- 首次上传时会自动创建 Gist，后续会更新同一个 Gist

## 查看统计数据

### 方法1：使用 HTML 查看器（推荐）

1. 打开项目根目录的 `stats_viewer.html` 文件
2. 在浏览器中打开该文件
3. 输入你的 GitHub Token
4. （可选）如果知道 Gist ID，也可以直接输入
5. 点击 "加载统计数据" 按钮
6. 查看统计信息

### 方法2：直接访问 Gist

1. 首次上传后，应用会在日志中输出 Gist ID
2. 访问：`https://gist.github.com/你的用户名/GistID`
3. 查看 JSON 格式的统计数据

### 方法3：使用 GitHub API

```bash
# 获取 Gist 列表
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.github.com/gists

# 读取特定 Gist
curl -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.github.com/gists/GIST_ID
```

## 数据格式

统计数据以 JSON 格式存储，包含以下信息：

```json
[
  {
    "version": "2.0.2",
    "versionCode": 22,
    "deviceId": "设备唯一标识",
    "firstLaunch": 1234567890000,
    "lastLaunch": 1234567890000,
    "launchCount": 100,
    "totalSessionTime": 3600000,
    "uniqueDays": 30,
    "lastUpdate": 1234567890000,
    "sessions": []
  }
]
```

## 隐私说明

- 所有数据存储在**私有 GitHub Gist**中，只有你可以访问
- 设备 ID 经过哈希处理，不包含敏感信息
- 不收集任何个人信息或位置信息
- 数据仅用于统计使用情况

## 故障排除

### 统计数据未上传

1. 检查 `gradle.properties` 中是否配置了 `GITHUB_TOKEN`
2. 检查 Token 是否有 `gist` 权限
3. 查看应用日志（使用 `adb logcat` 或 Android Studio Logcat）
4. 搜索关键字：`StatsCollector` 或 `GistUploader`

### 无法查看统计数据

1. 确认 Token 有效且未过期
2. 确认 Token 有 `gist` 权限
3. 检查网络连接
4. 查看浏览器控制台错误信息

### 手动触发上传

如果需要立即上传统计数据（用于测试），可以在应用代码中调用：

```kotlin
statsCollector?.forceUpload()
```

## 注意事项

- 上传频率限制：每 24 小时上传一次，避免频繁请求 GitHub API
- GitHub API 限制：使用 Token 时，每小时 5000 次请求
- Gist 大小限制：单个文件最大 1MB
- 数据保留：数据会一直保留在 Gist 中，除非手动删除

## 技术细节

- **数据收集**：`AppUsageTracker` - 使用 SharedPreferences 轻量级存储
- **数据上传**：`GistUploader` - 通过 GitHub Gist API 上传
- **数据整合**：`StatsCollector` - 整合收集和上传逻辑
- **多设备支持**：所有设备共享同一个 Gist，每个设备的数据独立存储

