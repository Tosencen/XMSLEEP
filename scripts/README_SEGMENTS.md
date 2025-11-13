# 音频分段播放资源准备指南

## 概述

实现分段播放需要将音频文件拆分成多个片段。这个过程可以**分阶段进行**，不需要一次性完成所有音频。

## 时间估算

- **单个音频文件分段**: 5-10 分钟（包括分割和测试）
- **批量处理（10个音频）**: 1-2 小时（使用脚本自动化）
- **手动处理（10个音频）**: 3-5 小时

## 推荐方案：先测试一个音频

### 步骤 1: 选择一个测试音频

选择一个较短的音频文件进行测试，比如 `heavy_rain.ogg`。

### 步骤 2: 使用脚本分割音频

```bash
# 1. 给脚本添加执行权限
chmod +x scripts/split_audio.sh

# 2. 分割音频（分成 5 个片段）
./scripts/split_audio.sh audio/rain/heavy_rain.ogg 5 audio/rain/heavy_rain_segments/
```

### 步骤 3: 生成片段元数据

```bash
# 生成 JSON 元数据
python3 scripts/generate_segments_metadata.py \
    heavy_rain \
    audio/rain/heavy_rain_segments/ \
    heavy_rain_segments.json
```

### 步骤 4: 在代码中使用

```kotlin
// 读取 JSON 并设置片段
val segmentsJson = // 从文件读取
val segments = // 解析 JSON 为 SoundSegment 列表
AudioManager.getInstance().setLocalSegments(Sound.HEAVY_RAIN, segments)
```

## 批量处理方案

如果有很多音频需要处理，可以创建一个批量处理脚本：

```bash
#!/bin/bash
# 批量处理脚本示例

AUDIOS=(
    "audio/rain/heavy_rain.ogg:5"
    "audio/nature/river.ogg:4"
    "audio/nature/waves.ogg:5"
)

for audio_info in "${AUDIOS[@]}"; do
    IFS=':' read -r audio_file segment_count <<< "$audio_info"
    audio_name=$(basename "$audio_file" .ogg)
    output_dir="audio/segments/${audio_name}_segments/"
    
    echo "处理: $audio_file"
    ./scripts/split_audio.sh "$audio_file" "$segment_count" "$output_dir"
    python3 scripts/generate_segments_metadata.py "$audio_name" "$output_dir" "${audio_name}_segments.json"
done
```

## 手动处理方案（如果不想用脚本）

### 使用 Audacity（图形界面）

1. 打开音频文件
2. 选择要分割的部分
3. 导出选中的音频
4. 重复直到分割完所有片段

### 使用在线工具

- [AudioMass](https://audiomass.co/) - 在线音频编辑器
- [MP3Cut](https://mp3cut.net/) - 在线音频切割工具

## 片段数量建议

- **短音频（< 2分钟）**: 3-4 个片段
- **中等音频（2-5分钟）**: 4-6 个片段
- **长音频（> 5分钟）**: 6-10 个片段

## 注意事项

1. **无缝循环**: 确保每个片段的首尾能够无缝衔接
2. **文件大小**: 片段文件会增加 APK 大小，考虑使用远程片段
3. **测试**: 每个音频分段后都要测试播放是否正常

## 渐进式实现策略

1. **第一阶段**: 只处理 1-2 个热门音频（如 heavy_rain, river）
2. **第二阶段**: 根据用户反馈，逐步添加更多音频的分段
3. **第三阶段**: 如果效果好，批量处理剩余音频

这样可以在**1-2小时内**完成第一阶段，验证功能是否正常，然后再决定是否继续。

