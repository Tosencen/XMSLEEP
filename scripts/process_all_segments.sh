#!/bin/bash

# 完整的音频片段处理流程
# 1. 批量拆分音频文件
# 2. 生成远程片段元数据
# 用法: ./process_all_segments.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BATCH_SPLIT_SCRIPT="$SCRIPT_DIR/batch_split_audio.sh"
GENERATE_METADATA_SCRIPT="$SCRIPT_DIR/generate_remote_segments_metadata.sh"

echo "=========================================="
echo "完整的音频片段处理流程"
echo "=========================================="
echo ""

# 步骤 1: 批量拆分音频文件
echo "步骤 1/2: 批量拆分音频文件..."
echo "----------------------------------------"
if [ -f "$BATCH_SPLIT_SCRIPT" ]; then
    "$BATCH_SPLIT_SCRIPT"
    if [ $? -ne 0 ]; then
        echo "错误: 批量拆分失败"
        exit 1
    fi
else
    echo "错误: 找不到 batch_split_audio.sh"
    exit 1
fi

echo ""
echo "步骤 2/2: 生成远程片段元数据..."
echo "----------------------------------------"
if [ -f "$GENERATE_METADATA_SCRIPT" ]; then
    "$GENERATE_METADATA_SCRIPT"
    if [ $? -ne 0 ]; then
        echo "错误: 生成元数据失败"
        exit 1
    fi
else
    echo "错误: 找不到 generate_remote_segments_metadata.sh"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 所有步骤完成！"
echo "=========================================="
echo ""
echo "生成的文件："
echo "  - 片段文件: audio_segments/"
echo "  - 元数据文件: segments_metadata/"
echo ""
echo "下一步操作："
echo ""
echo "1. 检查生成的文件："
echo "   ls -la audio_segments/"
echo "   ls -la segments_metadata/"
echo ""
echo "2. 上传到 GitHub："
echo "   git add audio_segments/ segments_metadata/"
echo "   git commit -m 'Add audio segments and metadata for all sounds'"
echo "   git push origin main"
echo ""
echo "3. 验证远程 URL（上传后）："
echo "   https://cdn.jsdelivr.net/gh/Tosencen/XMSLEEP@main/segments_metadata/heavy_rain_segments.json"
echo ""

