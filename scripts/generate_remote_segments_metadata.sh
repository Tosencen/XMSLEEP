#!/bin/bash

# 批量生成远程音频片段元数据
# 用法: ./generate_remote_segments_metadata.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
METADATA_SCRIPT="$SCRIPT_DIR/generate_segments_metadata.py"
SEGMENTS_DIR="$PROJECT_ROOT/audio_segments"
METADATA_DIR="$PROJECT_ROOT/segments_metadata"

# GitHub 仓库信息
GITHUB_USER="Tosencen"
GITHUB_REPO="XMSLEEP"
GITHUB_BRANCH="main"
BASE_URL="https://cdn.jsdelivr.net/gh/$GITHUB_USER/$GITHUB_REPO@$GITHUB_BRANCH/audio_segments"

# 检查脚本是否存在
if [ ! -f "$METADATA_SCRIPT" ]; then
    echo "错误: 找不到 generate_segments_metadata.py"
    exit 1
fi

# 创建元数据目录
mkdir -p "$METADATA_DIR"

echo "=========================================="
echo "批量生成远程音频片段元数据"
echo "=========================================="
echo "GitHub 仓库: $GITHUB_USER/$GITHUB_REPO"
echo "分支: $GITHUB_BRANCH"
echo "基础 URL: $BASE_URL"
echo ""

# 检查片段目录是否存在
if [ ! -d "$SEGMENTS_DIR" ]; then
    echo "错误: 片段目录不存在: $SEGMENTS_DIR"
    echo "请先运行 ./scripts/batch_split_audio.sh 生成片段文件"
    exit 1
fi

# 查找所有片段目录
segment_dirs=$(find "$SEGMENTS_DIR" -type d -name "*_segments" | sort)

if [ -z "$segment_dirs" ]; then
    echo "警告: 未找到任何片段目录"
    echo "请先运行 ./scripts/batch_split_audio.sh 生成片段文件"
    exit 1
fi

total=0
success=0
failed=0

# 处理每个片段目录
for segment_dir in $segment_dirs; do
    total=$((total + 1))
    
    # 从路径中提取音频ID
    # 例如: audio_segments/heavy_rain_segments -> heavy_rain
    dir_name=$(basename "$segment_dir")
    audio_id="${dir_name%_segments}"
    
    # 生成元数据文件路径
    metadata_file="$METADATA_DIR/${audio_id}_segments.json"
    
    # 生成远程 URL 基础路径
    remote_base_url="$BASE_URL/${dir_name}"
    
    echo "[$total] 处理: $audio_id"
    echo "  片段目录: $segment_dir"
    echo "  元数据文件: $metadata_file"
    echo "  远程 URL: $remote_base_url"
    
    # 检查片段目录是否有文件
    segment_count=$(find "$segment_dir" -type f -name "*_segment_*" | wc -l | tr -d ' ')
    if [ "$segment_count" -eq 0 ]; then
        echo "  ⚠️  跳过: 片段目录为空"
        failed=$((failed + 1))
        continue
    fi
    
    # 生成元数据
    if python3 "$METADATA_SCRIPT" "$audio_id" "$segment_dir" "$metadata_file" "$remote_base_url"; then
        echo "  ✓ 成功生成元数据（$segment_count 个片段）"
        success=$((success + 1))
    else
        echo "  ✗ 生成元数据失败"
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
echo "元数据文件保存在: $METADATA_DIR"
echo ""
echo "下一步："
echo "1. 检查生成的元数据文件"
echo "2. 上传到 GitHub:"
echo "   git add audio_segments/ segments_metadata/"
echo "   git commit -m 'Add audio segments and metadata'"
echo "   git push origin main"
echo ""
echo "3. 在应用中使用："
echo "   - 从远程 URL 加载元数据 JSON"
echo "   - 使用 AudioManager.setRemoteSegments() 设置片段"

