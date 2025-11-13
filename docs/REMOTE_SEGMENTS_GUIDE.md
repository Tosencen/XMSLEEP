# 远程音频片段使用指南

## 概述

远程音频片段允许你将音频文件拆分成多个片段，上传到 GitHub，然后通过 CDN（如 jsDelivr）加载播放。这样可以：
- 减小 APK 大小
- 支持动态更新音频内容
- 提供更好的加载性能（CDN 加速）

## 工作流程

### 1. 准备音频片段

使用脚本批量处理音频文件：

```bash
# 批量处理所有音频文件
./scripts/batch_split_audio.sh
```

这会：
- 将音频文件拆分成多个片段
- 生成片段元数据 JSON 文件
- 保存在 `audio_segments/` 和 `segments_metadata/` 目录

### 2. 上传到 GitHub

**是的，你需要将音频片段和 JSON 文件同步到 GitHub！**

#### 2.1 创建 GitHub 仓库结构

建议的目录结构：

```
your-repo/
├── audio/
│   ├── rain/
│   │   ├── heavy_rain_segments/
│   │   │   ├── heavy_rain_segment_0.ogg
│   │   │   ├── heavy_rain_segment_1.ogg
│   │   │   ├── heavy_rain_segment_2.ogg
│   │   │   └── heavy_rain_segment_3.ogg
│   │   └── heavy_rain.ogg
│   └── nature/
│       └── river_segments/
│           ├── river_segment_0.ogg
│           └── ...
└── segments_metadata/
    ├── heavy_rain_segments.json
    └── river_segments.json
```

#### 2.2 生成远程片段元数据

使用脚本生成包含远程 URL 的元数据：

```bash
# 生成远程片段元数据（指定 GitHub base URL）
python3 scripts/generate_segments_metadata.py \
    heavy_rain \
    audio_segments/heavy_rain_segments/ \
    segments_metadata/heavy_rain_segments.json \
    https://cdn.jsdelivr.net/gh/yourusername/your-repo@main/audio/rain/heavy_rain_segments
```

这会生成包含 `remoteUrl` 的 JSON：

```json
{
  "audioId": "heavy_rain",
  "segments": [
    {
      "name": "heavy_rain_segment_0",
      "basePath": "audio/rain/heavy_rain_segments/heavy_rain_segment_0.ogg",
      "isFree": true,
      "remoteUrl": "https://cdn.jsdelivr.net/gh/yourusername/your-repo@main/audio/rain/heavy_rain_segments/heavy_rain_segment_0.ogg"
    },
    ...
  ]
}
```

#### 2.3 上传到 GitHub

```bash
# 1. 添加音频片段文件
git add audio_segments/
git commit -m "Add audio segments"
git push

# 2. 添加元数据文件
git add segments_metadata/
git commit -m "Add segments metadata"
git push
```

### 3. 在应用中使用

#### 3.1 加载远程片段元数据

```kotlin
// 从远程 URL 加载片段元数据
val segmentsJsonUrl = "https://cdn.jsdelivr.net/gh/yourusername/your-repo@main/segments_metadata/heavy_rain_segments.json"

// 使用网络库（如 Retrofit）加载 JSON
val segments = loadSegmentsFromRemote(segmentsJsonUrl)

// 设置远程片段
AudioManager.getInstance().setRemoteSegments("heavy_rain", segments)
```

#### 3.2 播放远程音频

```kotlin
// 播放远程音频（如果设置了片段，会自动使用片段播放）
AudioManager.getInstance().playRemoteSound(
    context,
    metadata,
    uri
)
```

## URL 格式

### GitHub Raw URL
```
https://raw.githubusercontent.com/username/repo/branch/path/to/file.ogg
```

### jsDelivr CDN URL（推荐，国内可访问）
```
https://cdn.jsdelivr.net/gh/username/repo@branch/path/to/file.ogg
```

**注意**：项目已经实现了自动 URL 转换，GitHub raw URL 会自动转换为 jsDelivr CDN URL。

## 完整示例

### 1. 批量处理并生成元数据

```bash
# 处理所有音频
./scripts/batch_split_audio.sh

# 为每个音频生成远程元数据
for audio in heavy_rain river waves; do
    python3 scripts/generate_segments_metadata.py \
        $audio \
        audio_segments/${audio}_segments/ \
        segments_metadata/${audio}_segments.json \
        https://cdn.jsdelivr.net/gh/yourusername/your-repo@main/audio/${audio}_segments
done
```

### 2. 上传到 GitHub

```bash
git add audio_segments/ segments_metadata/
git commit -m "Add audio segments and metadata"
git push origin main
```

### 3. 在应用中加载

```kotlin
// 在应用启动时加载片段元数据
suspend fun loadRemoteSegments() {
    val segmentsUrl = "https://cdn.jsdelivr.net/gh/yourusername/your-repo@main/segments_metadata/heavy_rain_segments.json"
    val segments = loadSegmentsFromRemote(segmentsUrl)
    AudioManager.getInstance().setRemoteSegments("heavy_rain", segments)
}
```

## 注意事项

1. **文件大小**：确保片段文件不会太大，建议每个片段 < 5MB
2. **CDN 缓存**：jsDelivr 有缓存，更新文件后可能需要等待几分钟
3. **网络错误**：应用已经实现了错误重试机制
4. **回退机制**：如果 jsDelivr 失败，会自动回退到 GitHub raw URL

## 本地 vs 远程片段

- **本地片段**：打包在 APK 中，无需网络，但增加 APK 大小
- **远程片段**：从 CDN 加载，减小 APK 大小，但需要网络连接

可以根据需要混合使用：
- 热门音频使用本地片段（快速加载）
- 其他音频使用远程片段（减小 APK 大小）

