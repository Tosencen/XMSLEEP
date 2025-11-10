#!/bin/bash

# 上传 APK 到 GitHub Release v2.0.3
# 使用方法：先运行 gh auth login 登录，然后运行此脚本

echo "正在上传 APK 到 GitHub Release v2.0.3..."

# 上传 APK 文件
gh release upload v2.0.3 \
  ~/Desktop/XMSLEEP-v2.0.3-release.apk \
  --repo Tosencen/XMSLEEP \
  --clobber

# 更新 release 说明
gh release edit v2.0.3 \
  --repo Tosencen/XMSLEEP \
  --notes-file RELEASE_NOTES_v2.0.3.md

echo "✅ 上传完成！"
echo "请访问：https://github.com/Tosencen/XMSLEEP/releases/tag/v2.0.3"

