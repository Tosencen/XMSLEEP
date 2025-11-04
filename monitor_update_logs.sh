#!/bin/bash
# 监控UpdateDialog下载进度条日志

echo "=========================================="
echo "监控 UpdateDialog 下载进度条日志"
echo "=========================================="
echo ""
echo "请在应用中执行以下操作："
echo "1. 打开应用"
echo "2. 触发更新检查（如果有新版本会显示弹窗）"
echo "3. 点击'立即更新'按钮"
echo "4. 观察日志输出"
echo ""
echo "=========================================="
echo ""

# 清除之前的日志
adb logcat -c

# 监控相关日志标签
adb logcat -s UpdateDialog:D UpdateCheck:D UpdateViewModel:D UpdateInstaller:D FileDownloader:D | \
    grep -E "(状态|Downloading|进度|progress|显示弹窗|shouldShowUpdateDialog|LinearProgressIndicator)" --color=always
