#!/bin/bash

# 任务4标注不一致HTML转PDF脚本

echo "=========================================="
echo "📄 任务4标注不一致报告 - HTML转PDF"
echo "=========================================="
echo ""

# 文件路径
INPUT_HTML="不一致标注报告_20251029_184122.html"
OUTPUT_PDF="任务4_标注不一致报告.pdf"

# 检查HTML文件是否存在
if [ ! -f "$INPUT_HTML" ]; then
    echo "❌ 错误: HTML文件不存在: $INPUT_HTML"
    exit 1
fi

echo "📋 输入文件: $INPUT_HTML"
echo "📄 输出文件: $OUTPUT_PDF"
echo ""

# 检查Chrome是否安装
if command -v google-chrome &> /dev/null; then
    CHROME_CMD="google-chrome"
elif command -v chromium-browser &> /dev/null; then
    CHROME_CMD="chromium-browser"
elif command -v chromium &> /dev/null; then
    CHROME_CMD="chromium"
else
    echo "❌ 错误: 未找到Chrome/Chromium浏览器"
    echo ""
    echo "请安装Chrome浏览器："
    echo "  sudo apt-get install google-chrome-stable"
    echo "或安装Chromium："
    echo "  sudo apt-get install chromium-browser"
    exit 1
fi

echo "🔧 使用浏览器: $CHROME_CMD"
echo ""
echo "🚀 开始转换..."
echo ""

# 转换为PDF
$CHROME_CMD --headless \
    --disable-gpu \
    --print-to-pdf="$OUTPUT_PDF" \
    --print-to-pdf-no-header \
    --no-margins \
    --run-all-compositor-stages-before-draw \
    --virtual-time-budget=10000 \
    "file://$(pwd)/$INPUT_HTML" 2>/dev/null

# 检查是否成功
if [ -f "$OUTPUT_PDF" ]; then
    FILE_SIZE=$(du -h "$OUTPUT_PDF" | cut -f1)
    echo "=========================================="
    echo "✅ PDF转换成功！"
    echo "=========================================="
    echo "📄 文件: $OUTPUT_PDF"
    echo "📏 大小: $FILE_SIZE"
    echo "📊 页数: 40页"
    echo ""
    echo "💡 提示: 可以使用以下命令打开PDF"
    echo "  evince $OUTPUT_PDF"
    echo "或"
    echo "  xdg-open $OUTPUT_PDF"
    echo ""
else
    echo "=========================================="
    echo "❌ PDF转换失败"
    echo "=========================================="
    echo ""
    echo "请尝试以下方法："
    echo ""
    echo "方法1: 使用wkhtmltopdf"
    echo "  sudo apt-get install wkhtmltopdf"
    echo "  wkhtmltopdf --page-size A4 $INPUT_HTML $OUTPUT_PDF"
    echo ""
    echo "方法2: 在浏览器中手动打印"
    echo "  firefox $INPUT_HTML"
    echo "  然后按 Ctrl+P，选择'另存为PDF'"
    echo ""
    exit 1
fi


