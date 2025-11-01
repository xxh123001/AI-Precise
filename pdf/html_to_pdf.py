#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import subprocess
import sys
import os

def check_and_install_weasyprint():
    """检查并安装weasyprint"""
    try:
        import weasyprint
        print("✓ weasyprint 已安装")
        return True
    except ImportError:
        print("正在安装 weasyprint (使用清华源)...")
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", 
            "-i", "https://pypi.tuna.tsinghua.edu.cn/simple",
            "weasyprint"
        ])
        print("✓ weasyprint 安装完成")
        return True

def html_to_pdf(html_file, pdf_file):
    """将HTML转换为PDF"""
    from weasyprint import HTML, CSS
    
    print(f"\n开始转换HTML到PDF...")
    print(f"输入: {html_file}")
    print(f"输出: {pdf_file}")
    
    # 添加打印样式
    print_css = CSS(string='''
        @page {
            size: A4;
            margin: 1cm;
        }
        
        .page {
            page-break-after: always;
            page-break-inside: avoid;
        }
        
        .image-container img {
            max-width: 100%;
            height: auto;
        }
    ''')
    
    try:
        HTML(filename=html_file).write_pdf(
            pdf_file,
            stylesheets=[print_css]
        )
        print(f"\n✅ PDF转换成功！")
        print(f"📄 输出文件: {pdf_file}")
        
        # 获取文件大小
        file_size = os.path.getsize(pdf_file) / (1024 * 1024)
        print(f"📊 文件大小: {file_size:.2f} MB")
        
        return True
    except Exception as e:
        print(f"\n❌ 转换失败: {e}")
        return False

def main():
    # 检查并安装依赖
    if not check_and_install_weasyprint():
        print("无法安装必要的依赖")
        return
    
    # 文件路径
    html_file = '/Users/felix/Desktop/inconsistent_annotations/output_pdfs/inconsistent_annotations_all.html'
    pdf_file = '/Users/felix/Desktop/inconsistent_annotations/output_pdfs/inconsistent_annotations_from_html.pdf'
    
    # 检查HTML文件是否存在
    if not os.path.exists(html_file):
        print(f"❌ HTML文件不存在: {html_file}")
        return
    
    # 转换
    if html_to_pdf(html_file, pdf_file):
        print(f"\n{'='*60}")
        print("🎉 转换完成！可以打开PDF文件查看")
        print(f"{'='*60}")

if __name__ == '__main__':
    main()

