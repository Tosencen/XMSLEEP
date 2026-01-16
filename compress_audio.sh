#!/bin/bash

# 批量压缩 raw 文件夹中的音频文件
# 使用 ffmpeg 将 ogg 文件压缩到更小的体积

RAW_DIR="app/src/main/res/raw"
BACKUP_DIR="app/src/main/res/raw_backup"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

echo "开始压缩音频文件..."
echo "原始文件将备份到: $BACKUP_DIR"
echo ""

# 统计信息
total_original_size=0
total_compressed_size=0
file_count=0

# 遍历所有 ogg 文件
for file in "$RAW_DIR"/*.ogg; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        
        # 跳过已经很小的文件（小于 500KB）
        filesize=$(stat -f%z "$file")
        if [ $filesize -lt 512000 ]; then
            echo "⏭️  跳过 $filename (已经很小: $(numfmt --to=iec-i --suffix=B $filesize))"
            continue
        fi
        
        # 备份原文件
        cp "$file" "$BACKUP_DIR/"
        
        # 压缩文件
        temp_file="${file}.tmp.ogg"
        
        echo "🔄 压缩: $filename"
        echo "   原始大小: $(numfmt --to=iec-i --suffix=B $filesize)"
        
        # 使用 ffmpeg 压缩
        # -q:a 4 表示质量等级（0-10，数字越小质量越高，4是较好的平衡点）
        ffmpeg -i "$file" -c:a libvorbis -q:a 4 "$temp_file" -y -loglevel error
        
        if [ $? -eq 0 ]; then
            new_filesize=$(stat -f%z "$temp_file")
            reduction=$((filesize - new_filesize))
            percentage=$((reduction * 100 / filesize))
            
            echo "   压缩后: $(numfmt --to=iec-i --suffix=B $new_filesize)"
            echo "   减少: $(numfmt --to=iec-i --suffix=B $reduction) ($percentage%)"
            
            # 替换原文件
            mv "$temp_file" "$file"
            
            total_original_size=$((total_original_size + filesize))
            total_compressed_size=$((total_compressed_size + new_filesize))
            file_count=$((file_count + 1))
        else
            echo "   ❌ 压缩失败"
            rm -f "$temp_file"
        fi
        
        echo ""
    fi
done

# 显示总结
echo "================================"
echo "压缩完成！"
echo "处理文件数: $file_count"
if [ $file_count -gt 0 ]; then
    echo "原始总大小: $(numfmt --to=iec-i --suffix=B $total_original_size)"
    echo "压缩后总大小: $(numfmt --to=iec-i --suffix=B $total_compressed_size)"
    total_reduction=$((total_original_size - total_compressed_size))
    total_percentage=$((total_reduction * 100 / total_original_size))
    echo "总共减少: $(numfmt --to=iec-i --suffix=B $total_reduction) ($total_percentage%)"
fi
echo ""
echo "原始文件已备份到: $BACKUP_DIR"
echo "如果压缩效果不满意，可以从备份恢复"
