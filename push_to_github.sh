#!/bin/bash

# AI-Precise 图像标注系统 - GitHub 推送脚本
# Version: 1.0.1-release

echo "=========================================="
echo "AI-Precise 图像标注系统 GitHub 推送"
echo "Version: 1.0.1-release"
echo "=========================================="
echo ""

# 检查是否在正确的目录
if [ ! -d ".git" ]; then
    echo "❌ 错误：当前不在 git 仓库目录中"
    exit 1
fi

# 显示当前分支
CURRENT_BRANCH=$(git branch --show-current)
echo "📍 当前分支: $CURRENT_BRANCH"
echo ""

# 显示远程仓库
REMOTE_URL=$(git remote get-url origin)
echo "🔗 远程仓库: $REMOTE_URL"
echo ""

# 显示文件统计
FILE_COUNT=$(git ls-files | wc -l)
echo "📊 文件统计: $FILE_COUNT 个文件"
echo ""

# 推送到远程仓库
echo "🚀 开始推送到 GitHub..."
echo ""

# 推送当前分支
git push -u origin $CURRENT_BRANCH

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ 推送成功！"
    echo "=========================================="
    echo ""
    echo "📦 分支: $CURRENT_BRANCH"
    echo "🔗 仓库: https://github.com/xxh123001/AI-Precise"
    echo "🌿 查看: https://github.com/xxh123001/AI-Precise/tree/$CURRENT_BRANCH"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "❌ 推送失败"
    echo "=========================================="
    echo ""
    echo "可能的原因："
    echo "1. GitHub 认证未配置"
    echo "2. 网络连接问题"
    echo "3. 仓库权限不足"
    echo ""
    echo "解决方案："
    echo "1. 配置 GitHub Token:"
    echo "   git remote set-url origin https://YOUR_TOKEN@github.com/xxh123001/AI-Precise.git"
    echo ""
    echo "2. 或使用 SSH:"
    echo "   git remote set-url origin git@github.com:xxh123001/AI-Precise.git"
    echo ""
    exit 1
fi

