# 音频片段处理状态

## 当前状态

### ✅ 已处理的音频

1. **heavy_rain** (大雨)
   - 片段目录: `audio/rain/heavy_rain_segments/`
   - 元数据文件: `heavy_rain_segments.json`
   - 状态: ✅ 已完成

### ❌ 待处理的音频

根据 `scripts/batch_split_audio.sh` 配置，以下音频需要处理：

#### Rain (雨声)
- [ ] `rain/light-rain.ogg` (4 片段)

#### Nature (自然)
- [ ] `nature/river.ogg` (5 片段)
- [ ] `nature/waves.ogg` (5 片段)
- [ ] `nature/wind.ogg` (4 片段)
- [ ] `nature/wind-in-trees.ogg` (4 片段)
- [ ] `nature/campfire_new.ogg` (5 片段)

#### Animals (动物)
- [ ] `animals/birds.ogg` (4 片段)
- [ ] `animals/crickets.ogg` (4 片段)

#### Things (物品)
- [ ] `things/keyboard.ogg` (4 片段)
- [ ] `things/typewriter.ogg` (4 片段)
- [ ] `things/clock.ogg` (3 片段)

#### Places (场所)
- [ ] `places/library.mp3` (4 片段)
- [ ] `places/office.mp3` (4 片段)
- [ ] `places/cafe.mp3` (4 片段)

#### Transport (交通)
- [ ] `transport/train.mp3` (5 片段)
- [ ] `transport/airplane.mp3` (5 片段)

## 批量处理步骤

### 1. 运行批量处理脚本

```bash
# 确保脚本有执行权限
chmod +x scripts/batch_split_audio.sh

# 运行批量处理
./scripts/batch_split_audio.sh
```

这会：
- 处理所有配置的音频文件
- 生成片段文件到 `audio_segments/` 目录
- 生成元数据 JSON 到 `segments_metadata/` 目录

### 2. 生成远程片段元数据（用于 GitHub）

处理完本地片段后，需要为每个音频生成包含远程 URL 的元数据：

```bash
# 设置 GitHub 仓库信息
GITHUB_USER="Tosencen"
GITHUB_REPO="XMSLEEP"
GITHUB_BRANCH="main"

# 为每个音频生成远程元数据
for audio_id in heavy_rain light_rain river waves wind wind_in_trees campfire birds crickets keyboard typewriter clock library office cafe train airplane; do
    python3 scripts/generate_segments_metadata.py \
        $audio_id \
        audio_segments/${audio_id}_segments/ \
        segments_metadata/${audio_id}_segments.json \
        https://cdn.jsdelivr.net/gh/$GITHUB_USER/$GITHUB_REPO@$GITHUB_BRANCH/audio_segments/${audio_id}_segments
done
```

### 3. 上传到 GitHub

```bash
# 添加片段文件和元数据
git add audio_segments/ segments_metadata/
git commit -m "Add audio segments and metadata for all sounds"
git push origin main
```

## 注意事项

1. **文件大小**：确保片段文件不会太大，建议每个片段 < 5MB
2. **Git LFS**：如果文件很大，考虑使用 Git LFS
3. **CDN 缓存**：上传后，jsDelivr CDN 可能需要几分钟更新缓存
4. **测试**：上传后记得测试远程加载是否正常

## 快速检查命令

```bash
# 检查已处理的片段数量
find audio -type d -name "*_segments" | wc -l

# 检查已生成的元数据文件数量
find . -name "*_segments.json" -type f | wc -l

# 查看所有待处理的音频
grep -E "^\s+\"[^:]+\.(ogg|mp3)" scripts/batch_split_audio.sh
```

