#!/bin/bash

# 批量处理音频文件，拆分成多个片段
# 用法: ./batch_split_audio.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SPLIT_SCRIPT="$SCRIPT_DIR/split_audio.sh"
METADATA_SCRIPT="$SCRIPT_DIR/generate_segments_metadata.py"
AUDIO_DIR="$PROJECT_ROOT/audio"

# 检查脚本是否存在
if [ ! -f "$SPLIT_SCRIPT" ]; then
    echo "错误: 找不到 split_audio.sh"
    exit 1
fi

if [ ! -f "$METADATA_SCRIPT" ]; then
    echo "错误: 找不到 generate_segments_metadata.py"
    exit 1
fi

# 给脚本添加执行权限
chmod +x "$SPLIT_SCRIPT"

# 定义要处理的音频文件及其片段数量
# 格式: "相对路径:片段数量:音频ID"
declare -a AUDIO_CONFIGS=(
    # Rain
    "rain/heavy_rain.ogg:4:heavy_rain"
    "rain/light-rain.ogg:4:light_rain"
    
    # Nature
    "nature/river.ogg:5:river"
    "nature/waves.ogg:5:waves"
    "nature/wind.ogg:4:wind"
    "nature/wind-in-trees.ogg:4:wind_in_trees"
    "nature/campfire_new.ogg:5:campfire"
    
    # Animals
    "animals/birds.ogg:4:birds"
    "animals/crickets.ogg:4:crickets"
    
    # Things
    "things/keyboard.ogg:4:keyboard"
    "things/typewriter.ogg:4:typewriter"
    "things/clock.ogg:3:clock"
    
    # Places
    "places/library.mp3:4:library"
    "places/office.mp3:4:office"
    "places/cafe.mp3:4:cafe"
    
    # Transport
    "transport/train.mp3:5:train"
    "transport/airplane.mp3:5:airplane"
)

# 创建输出目录
SEGMENTS_DIR="$PROJECT_ROOT/audio_segments"
METADATA_DIR="$PROJECT_ROOT/segments_metadata"
mkdir -p "$SEGMENTS_DIR"
mkdir -p "$METADATA_DIR"

echo "=========================================="
echo "批量处理音频文件 - 分段播放"
echo "=========================================="
echo ""

total=${#AUDIO_CONFIGS[@]}
current=0
success=0
failed=0

for config in "${AUDIO_CONFIGS[@]}"; do
    current=$((current + 1))
    
    # 解析配置
    IFS=':' read -r audio_path segment_count audio_id <<< "$config"
    audio_file="$AUDIO_DIR/$audio_path"
    
    # 检查文件是否存在
    if [ ! -f "$audio_file" ]; then
        echo "[$current/$total] ⚠️  跳过: 文件不存在 - $audio_file"
        failed=$((failed + 1))
        continue
    fi
    
    # 创建输出目录
    audio_name=$(basename "$audio_file" | sed 's/\.[^.]*$//')
    output_dir="$SEGMENTS_DIR/${audio_name}_segments"
    
    echo "[$current/$total] 处理: $audio_id"
    echo "  文件: $audio_file"
    echo "  片段数: $segment_count"
    echo "  输出: $output_dir"
    
    # 分割音频
    if "$SPLIT_SCRIPT" "$audio_file" "$segment_count" "$output_dir"; then
        # 生成元数据
        metadata_file="$METADATA_DIR/${audio_id}_segments.json"
        if python3 "$METADATA_SCRIPT" "$audio_id" "$output_dir" "$metadata_file"; then
            echo "  ✓ 成功"
            success=$((success + 1))
        else
            echo "  ✗ 生成元数据失败"
            failed=$((failed + 1))
        fi
    else
        echo "  ✗ 分割失败"
        failed=$((failed + 1))
    fi
    
    echo ""
done

echo "=========================================="
echo "处理完成"
echo "=========================================="
echo "总计: $total"
echo "成功: $success"
echo "失败: $failed"
echo ""
echo "片段文件保存在: $SEGMENTS_DIR"
echo "元数据文件保存在: $METADATA_DIR"
echo ""
echo "下一步："
echo "1. 将片段文件复制到 app/src/main/res/raw/ 或上传到远程服务器"
echo "2. 更新元数据中的 localResourceId 或 remoteUrl"
echo "3. 在代码中加载并使用片段元数据"

