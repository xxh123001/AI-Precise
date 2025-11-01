#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import subprocess
import os

def html_to_pdf_chrome(html_file, pdf_file):
    """使用Chrome将HTML转换为PDF"""
    
    print(f"\n开始转换HTML到PDF (使用Chrome)...")
    print(f"输入: {html_file}")
    print(f"输出: {pdf_file}")
    
    # 获取绝对路径
    html_path = os.path.abspath(html_file)
    pdf_path = os.path.abspath(pdf_file)
    
    # 使用Chrome headless模式转换
    chrome_paths = [
        '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
        '/Applications/Chromium.app/Contents/MacOS/Chromium',
    ]
    
    chrome_path = None
    for path in chrome_paths:
        if os.path.exists(path):
            chrome_path = path
            break
    
    if not chrome_path:
        print("❌ 未找到Chrome浏览器")
        print("请使用浏览器打开HTML文件，然后使用打印功能保存为PDF")
        return False
    
    try:
        cmd = [
            chrome_path,
            '--headless',
            '--disable-gpu',
            '--print-to-pdf=' + pdf_path,
            '--no-pdf-header-footer',
            '--print-to-pdf-no-header',
            'file://' + html_path
        ]
        
        subprocess.run(cmd, check=True, capture_output=True)
        
        print(f"\n✅ PDF转换成功！")
        print(f"📄 输出文件: {pdf_file}")
        
        # 获取文件大小
        if os.path.exists(pdf_file):
            file_size = os.path.getsize(pdf_file) / (1024 * 1024)
            print(f"📊 文件大小: {file_size:.2f} MB")
        
        return True
    except Exception as e:
        print(f"\n❌ 转换失败: {e}")
        return False

def main():
    # 文件路径
    html_file = '/Users/felix/Desktop/inconsistent_annotations/output_pdfs/inconsistent_annotations_all.html'
    pdf_file = '/Users/felix/Desktop/inconsistent_annotations/output_pdfs/inconsistent_annotations_from_html.pdf'
    
    # 检查HTML文件是否存在
    if not os.path.exists(html_file):
        print(f"❌ HTML文件不存在: {html_file}")
        return
    
    # 转换
    if html_to_pdf_chrome(html_file, pdf_file):
        print(f"\n{'='*60}")
        print("🎉 转换完成！")
        print(f"{'='*60}")
        
        # 打开PDF
        subprocess.run(['open', pdf_file])
    else:
        print("\n备选方案:")
        print(f"1. 在浏览器中打开: {html_file}")
        print(f"2. 按 Cmd+P 打印")
        print(f"3. 选择 '另存为PDF'")

if __name__ == '__main__':
    main()

