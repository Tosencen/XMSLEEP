#!/bin/bash

# 音频分段工具脚本
# 使用 FFmpeg 将音频文件分割成多个片段
# 用法: ./split_audio.sh <输入文件> <片段数量> <输出目录>

set -e

INPUT_FILE="$1"
SEGMENT_COUNT="$2"
OUTPUT_DIR="$3"

if [ -z "$INPUT_FILE" ] || [ -z "$SEGMENT_COUNT" ] || [ -z "$OUTPUT_DIR" ]; then
    echo "用法: $0 <输入文件> <片段数量> <输出目录>"
    echo "示例: $0 audio/nature/river.ogg 5 audio/nature/river_segments/"
    exit 1
fi

if [ ! -f "$INPUT_FILE" ]; then
    echo "错误: 文件不存在: $INPUT_FILE"
    exit 1
fi

# 检查 FFmpeg 是否安装
if ! command -v ffmpeg &> /dev/null; then
    echo "错误: 需要安装 FFmpeg"
    echo "macOS: brew install ffmpeg"
    echo "Ubuntu: sudo apt-get install ffmpeg"
    exit 1
fi

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 获取音频时长（秒）
DURATION=$(ffprobe -i "$INPUT_FILE" -show_entries format=duration -v quiet -of csv="p=0")
SEGMENT_DURATION=$(echo "$DURATION / $SEGMENT_COUNT" | bc -l)

echo "音频总时长: ${DURATION}秒"
echo "每个片段时长: ${SEGMENT_DURATION}秒"
echo "开始分割..."

# 获取文件名（不含扩展名）
BASENAME=$(basename "$INPUT_FILE" | sed 's/\.[^.]*$//')
EXTENSION="${INPUT_FILE##*.}"

# 分割音频
for i in $(seq 0 $((SEGMENT_COUNT - 1))); do
    START_TIME=$(echo "$i * $SEGMENT_DURATION" | bc -l)
    OUTPUT_FILE="${OUTPUT_DIR}/${BASENAME}_segment_${i}.${EXTENSION}"
    
    echo "生成片段 $((i+1))/$SEGMENT_COUNT: $OUTPUT_FILE"
    ffmpeg -i "$INPUT_FILE" -ss "$START_TIME" -t "$SEGMENT_DURATION" -c copy "$OUTPUT_FILE" -y -loglevel error
done

echo "完成！片段保存在: $OUTPUT_DIR"

