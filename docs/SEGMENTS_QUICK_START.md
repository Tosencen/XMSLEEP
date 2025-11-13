# 音频片段处理快速开始

## 一键处理所有音频

运行以下命令即可完成所有音频的片段处理和元数据生成：

```bash
./scripts/process_all_segments.sh
```

这个脚本会：
1. ✅ 批量拆分所有音频文件为片段
2. ✅ 生成包含远程 URL 的元数据 JSON

## 分步处理

### 步骤 1: 批量拆分音频

```bash
./scripts/batch_split_audio.sh
```

### 步骤 2: 生成远程元数据

```bash
./scripts/generate_remote_segments_metadata.sh
```

## 上传到 GitHub

处理完成后，上传到你的仓库：

```bash
# 添加所有文件
git add audio_segments/ segments_metadata/

# 提交
git commit -m "Add audio segments and metadata for all sounds"

# 推送到 GitHub
git push origin main
```

## 验证

上传后，可以通过以下 URL 访问元数据：

```
https://cdn.jsdelivr.net/gh/Tosencen/XMSLEEP@main/segments_metadata/heavy_rain_segments.json
```

## 在应用中使用

### 1. 加载远程片段元数据

```kotlin
// 从远程 URL 加载
val segmentsUrl = "https://cdn.jsdelivr.net/gh/Tosencen/XMSLEEP@main/segments_metadata/heavy_rain_segments.json"
val segments = loadSegmentsFromRemote(segmentsUrl)

// 设置远程片段
AudioManager.getInstance().setRemoteSegments("heavy_rain", segments)
```

### 2. 播放音频

```kotlin
// 播放时会自动使用片段播放
AudioManager.getInstance().playRemoteSound(context, metadata, uri)
```

## 文件结构

处理完成后，项目结构如下：

```
XMSLEEP/
├── audio_segments/              # 音频片段文件
│   ├── heavy_rain_segments/
│   │   ├── heavy_rain_segment_0.ogg
│   │   ├── heavy_rain_segment_1.ogg
│   │   └── ...
│   ├── river_segments/
│   └── ...
├── segments_metadata/           # 片段元数据 JSON
│   ├── heavy_rain_segments.json
│   ├── river_segments.json
│   └── ...
└── scripts/
    ├── batch_split_audio.sh
    ├── generate_remote_segments_metadata.sh
    └── process_all_segments.sh
```

## 注意事项

1. **FFmpeg 必须安装**：脚本依赖 FFmpeg
2. **Python 3 必须安装**：元数据生成需要 Python 3
3. **网络连接**：上传到 GitHub 需要网络连接
4. **文件大小**：确保片段文件不会太大（建议 < 5MB/片段）
5. **CDN 缓存**：上传后，jsDelivr CDN 可能需要几分钟更新

## 故障排除

### 问题 1: FFmpeg 未安装

```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt-get install ffmpeg
```

### 问题 2: Python 3 未安装

```bash
# macOS (通常已安装)
python3 --version

# 如果未安装
brew install python3
```

### 问题 3: 权限错误

```bash
chmod +x scripts/*.sh
```

### 问题 4: 片段目录为空

确保先运行 `batch_split_audio.sh` 生成片段文件。

