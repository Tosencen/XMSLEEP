#!/bin/bash
# Git Commit Helper - 快速规范提交

echo "=========================================="
echo "  Git Commit Helper - 规范提交助手"
echo "=========================================="
echo ""

# 选择类型
echo "请选择提交类型:"
echo "1) feat     - 新功能"
echo "2) fix      - Bug修复"
echo "3) docs     - 文档"
echo "4) style    - 代码格式"
echo "5) refactor - 重构"
echo "6) perf     - 性能优化"
echo "7) test     - 测试"
echo "8) chore    - 构建/工具"
echo "9) ci       - CI配置"
echo "0) build    - 构建系统"
read -p "输入数字 (1-9,0): " type_choice

case $type_choice in
    1) type="feat";;
    2) type="fix";;
    3) type="docs";;
    4) type="style";;
    5) type="refactor";;
    6) type="perf";;
    7) type="test";;
    8) type="chore";;
    9) type="ci";;
    0) type="build";;
    *) echo "无效选择"; exit 1;;
esac

# 选择作用域（可选）
echo ""
echo "选择作用域 (可选，直接回车跳过):"
echo "1) audio    2) ui       3) theme    4) timer"
echo "5) settings 6) update   7) cache    8) 其他"
read -p "输入数字或直接回车: " scope_choice

scope=""
case $scope_choice in
    1) scope="audio";;
    2) scope="ui";;
    3) scope="theme";;
    4) scope="timer";;
    5) scope="settings";;
    6) scope="update";;
    7) scope="cache";;
    8) read -p "输入自定义作用域: " scope;;
    "") scope="";;
esac

# 输入描述
echo ""
read -p "简短描述 (不超过50字符): " subject

# 输入详细说明（可选）
echo ""
echo "详细说明 (可选，直接回车跳过，输入'.'结束多行输入):"
body=""
while IFS= read -r line; do
    [[ "$line" == "." ]] && break
    body+="$line"$'\n'
done

# 关联Issue（可选）
echo ""
read -p "关联Issue号 (可选，如: 23): " issue

# 构建提交信息
if [ -n "$scope" ]; then
    commit_msg="$type($scope): $subject"
else
    commit_msg="$type: $subject"
fi

if [ -n "$body" ]; then
    commit_msg+=$'\n\n'"$body"
fi

if [ -n "$issue" ]; then
    commit_msg+=$'\n\n'"Closes #$issue"
fi

# 显示并确认
echo ""
echo "=========================================="
echo "提交信息预览:"
echo "=========================================="
echo "$commit_msg"
echo "=========================================="
echo ""
read -p "确认提交? (y/n): " confirm

if [[ $confirm == [yY] ]]; then
    git commit -m "$commit_msg"
    echo "✅ 提交成功！"
else
    echo "❌ 已取消提交"
    exit 1
fi
