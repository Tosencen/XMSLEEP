#!/usr/bin/env python3
"""
清理 XMSLEEP 统计数据中的测试数据
将 Gist 文件内容清空为空的 JSON 数组 []
"""

import json
import sys
import requests
import os

def clear_gist_stats(token: str, gist_id: str):
    """清空 Gist 中的统计数据"""
    
    url = f"https://api.github.com/gists/{gist_id}"
    headers = {
        "Accept": "application/vnd.github.v3+json",
        "Authorization": f"Bearer {token}"
    }
    
    # 创建空的 JSON 数组
    empty_content = json.dumps([], ensure_ascii=False, indent=2)
    
    # 更新 Gist
    data = {
        "description": "XMSLEEP App Usage Statistics",
        "files": {
            "xmsleep_stats.json": {
                "content": empty_content
            }
        }
    }
    
    try:
        response = requests.patch(url, headers=headers, json=data)
        
        if response.status_code == 200:
            print("✅ 统计数据已清空！")
            print(f"   Gist ID: {gist_id}")
            print(f"   内容: {empty_content}")
            return True
        else:
            print(f"❌ 清空失败: {response.status_code}")
            print(f"   错误信息: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ 发生错误: {e}")
        return False


if __name__ == "__main__":
    # 从环境变量或命令行参数获取 Token 和 Gist ID
    token = os.environ.get("GITHUB_TOKEN") or (sys.argv[1] if len(sys.argv) > 1 else None)
    gist_id = os.environ.get("GIST_ID") or (sys.argv[2] if len(sys.argv) > 2 else None)
    
    if not token:
        print("❌ 请提供 GitHub Token")
        print("   使用方法:")
        print("   python3 clear_stats.py <GITHUB_TOKEN> <GIST_ID>")
        print("   或者设置环境变量:")
        print("   export GITHUB_TOKEN=your_token")
        print("   export GIST_ID=your_gist_id")
        sys.exit(1)
    
    if not gist_id:
        print("❌ 请提供 Gist ID")
        print("   使用方法:")
        print("   python3 clear_stats.py <GITHUB_TOKEN> <GIST_ID>")
        sys.exit(1)
    
    print("正在清空统计数据...")
    clear_gist_stats(token, gist_id)

