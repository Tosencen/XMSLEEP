#!/usr/bin/env python3
"""
生成音频片段元数据 JSON
用法: python3 generate_segments_metadata.py <音频ID> <片段目录> <输出JSON文件>
"""

import json
import sys
import os
from pathlib import Path

def generate_segments_metadata(audio_id, segments_dir, output_file, base_url=None):
    """
    生成片段元数据
    
    Args:
        audio_id: 音频ID（如 "river"）
        segments_dir: 片段文件所在目录
        output_file: 输出JSON文件路径
        base_url: 远程URL基础路径（可选，用于远程片段）
    """
    segments_dir = Path(segments_dir)
    
    if not segments_dir.exists():
        print(f"错误: 目录不存在: {segments_dir}")
        return
    
    # 获取所有片段文件（按名称排序）
    segment_files = sorted(segments_dir.glob("*_segment_*"))
    
    if not segment_files:
        print(f"警告: 在 {segments_dir} 中未找到片段文件")
        return
    
    segments = []
    
    for idx, segment_file in enumerate(segment_files):
        segment_name = f"{audio_id}_segment_{idx}"
        base_path = str(segment_file.relative_to(segments_dir.parent))
        
        segment_data = {
            "name": segment_name,
            "basePath": base_path,
            "isFree": True
        }
        
        # 如果是远程片段，添加 remoteUrl
        if base_url:
            segment_data["remoteUrl"] = f"{base_url}/{segment_file.name}"
        else:
            # 本地片段：需要手动设置 localResourceId
            # 这里只生成结构，实际资源ID需要在Android项目中设置
            segment_data["localResourceId"] = None
        
        segments.append(segment_data)
    
    # 生成元数据
    metadata = {
        "audioId": audio_id,
        "segments": segments,
        "segmentCount": len(segments)
    }
    
    # 保存到文件
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    
    print(f"✓ 已生成片段元数据: {output_file}")
    print(f"  音频ID: {audio_id}")
    print(f"  片段数量: {len(segments)}")
    print(f"  片段列表:")
    for seg in segments:
        print(f"    - {seg['name']}: {seg['basePath']}")

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("用法: python3 generate_segments_metadata.py <音频ID> <片段目录> <输出JSON文件> [远程URL基础路径]")
        print("示例（本地）: python3 generate_segments_metadata.py river audio/nature/river_segments/ river_segments.json")
        print("示例（远程）: python3 generate_segments_metadata.py river audio/nature/river_segments/ river_segments.json https://raw.githubusercontent.com/user/repo/main/audio/nature/river_segments")
        sys.exit(1)
    
    audio_id = sys.argv[1]
    segments_dir = sys.argv[2]
    output_file = sys.argv[3]
    base_url = sys.argv[4] if len(sys.argv) > 4 else None
    
    generate_segments_metadata(audio_id, segments_dir, output_file, base_url)

