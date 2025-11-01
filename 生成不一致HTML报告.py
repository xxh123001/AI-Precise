#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成任务4标注不一致的HTML报告
每个PDF一张网页，控制不超出
"""

import pandas as pd
import base64
import os
import mysql.connector
from datetime import datetime

# 数据库连接配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'image_tag_system',
    'charset': 'utf8mb4'
}

def get_db_connection():
    """创建数据库连接"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except Exception as e:
        print(f"❌ 数据库连接失败: {e}")
        return None

def get_image_path(image_id, conn):
    """从数据库获取图片路径"""
    cursor = conn.cursor(dictionary=True)
    query = "SELECT file_path FROM images WHERE id = %s"
    cursor.execute(query, (image_id,))
    result = cursor.fetchone()
    cursor.close()
    
    if result:
        # file_path 格式如: /api/files/xxx.jpg
        # 需要转换为实际路径
        file_path = result['file_path']
        if file_path.startswith('/api/files/'):
            filename = file_path.replace('/api/files/', '')
            return os.path.join('/data/hong/tag/images', filename)
    return None

def image_to_base64(image_path):
    """将图片转换为base64编码"""
    try:
        with open(image_path, 'rb') as f:
            return base64.b64encode(f.read()).decode()
    except Exception as e:
        print(f"  图片读取失败: {e}")
        return None

def generate_html_page(row, img_base64):
    """生成单页HTML内容 - 控制在一页内"""
    filename = row['文件名']
    
    if not img_base64:
        return f'<div class="error">图片加载失败: {filename}</div>'
    
    # 解析各组标注内容，限制显示长度
    anno_parts = row['各组标注内容'].split(' | ')
    
    # 将标注内容分成两列显示，节省空间
    anno_grid_html = '<div class="anno-grid">'
    for i in range(0, len(anno_parts), 2):
        anno_grid_html += '<div class="anno-row">'
        anno_grid_html += f'<div class="anno-item">{anno_parts[i]}</div>'
        if i + 1 < len(anno_parts):
            anno_grid_html += f'<div class="anno-item">{anno_parts[i + 1]}</div>'
        else:
            anno_grid_html += '<div class="anno-item"></div>'  # 空白占位
        anno_grid_html += '</div>'
    anno_grid_html += '</div>'
    
    # 生成单页HTML - 严格控制高度
    page_html = f'''
    <div class="page">
        <div class="title">{filename}</div>
        
        <div class="content-wrapper">
            <div class="image-container">
                <img src="data:image/jpeg;base64,{img_base64}" alt="{filename}">
            </div>
            
            <div class="info-section">
                <div class="info-box">
                    <div class="box-title">标注详情</div>
                    <div class="box-content">
                        <div class="stats-row">
                            <span><strong>不同标注类型数:</strong> {row['不同标注类型数']}</span>
                            <span><strong>总标注人数:</strong> {row['总标注人数']}</span>
                        </div>
                        <div class="group-info"><strong>标注分组:</strong> {row['标注分组详情']}</div>
                        <div class="anno-title"><strong>各组标注内容:</strong></div>
                        {anno_grid_html}
                    </div>
                </div>
            </div>
        </div>
    </div>
    '''
    
    return page_html

def main():
    print("="*80)
    print("📊 生成任务4标注不一致HTML报告")
    print("="*80)
    print()
    
    # 读取最新的不一致报告CSV
    csv_files = [f for f in os.listdir('/data/hong/tag') if f.startswith('任务4_标注不一致报告_') and f.endswith('.csv')]
    if not csv_files:
        print("❌ 未找到不一致报告CSV文件")
        return
    
    # 使用最新的文件
    csv_file = sorted(csv_files)[-1]
    csv_path = f'/data/hong/tag/{csv_file}'
    
    print(f"📋 读取CSV文件: {csv_file}")
    df_inconsistent = pd.read_csv(csv_path)
    print(f"✅ 共有 {len(df_inconsistent)} 张不一致图片")
    print()
    
    # 连接数据库
    print("📊 连接数据库...")
    conn = get_db_connection()
    if not conn:
        return
    
    # 创建输出目录
    output_dir = '/data/hong/tag/不一致报告HTML'
    os.makedirs(output_dir, exist_ok=True)
    
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    output_html = os.path.join(output_dir, f'不一致标注报告_{timestamp}.html')
    
    print(f"📝 开始生成HTML文件...")
    print(f"📄 输出文件: {output_html}")
    print(f"📊 需要处理: {len(df_inconsistent)} 张图片")
    print()
    
    # HTML头部 - 优化样式确保一页显示
    html_content = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>任务4标注不一致报告</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", 
                         "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif;
            background: #f5f5f5;
            padding: 0;
            line-height: 1.5;
        }
        
        .page {
            width: 210mm;
            height: 297mm;
            margin: 0 auto;
            background: white;
            padding: 15mm;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            page-break-after: always;
            page-break-inside: avoid;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        }
        
        .title {
            text-align: center;
            font-size: 18px;
            font-weight: bold;
            color: #1a1a1a;
            margin-bottom: 10px;
            padding-bottom: 8px;
            border-bottom: 2px solid #e0e0e0;
            flex-shrink: 0;
        }
        
        .content-wrapper {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-height: 0;
        }
        
        .image-container {
            text-align: center;
            background: #fafafa;
            padding: 10px;
            border-radius: 6px;
            margin-bottom: 10px;
            flex-shrink: 0;
            max-height: 140mm;
            overflow: hidden;
        }
        
        .image-container img {
            max-width: 100%;
            max-height: 130mm;
            width: auto;
            height: auto;
            border-radius: 4px;
            box-shadow: 0 2px 6px rgba(0,0,0,0.1);
        }
        
        .info-section {
            flex: 1;
            min-height: 0;
            overflow: hidden;
        }
        
        .info-box {
            border: 2.5px solid #2C5F2D;
            border-radius: 6px;
            background: #FAFFFE;
            padding: 12px;
            height: 100%;
            display: flex;
            flex-direction: column;
        }
        
        .box-title {
            font-size: 16px;
            font-weight: bold;
            color: #2C5F2D;
            margin-bottom: 10px;
            padding-bottom: 6px;
            border-bottom: 1px solid #e0e0e0;
            flex-shrink: 0;
        }
        
        .box-content {
            font-size: 12px;
            color: #333;
            flex: 1;
            overflow: hidden;
        }
        
        .stats-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            padding-bottom: 6px;
            border-bottom: 1px dashed #ddd;
        }
        
        .group-info {
            margin-bottom: 8px;
            line-height: 1.4;
            word-wrap: break-word;
        }
        
        .anno-title {
            margin-bottom: 6px;
        }
        
        .anno-grid {
            margin-top: 4px;
        }
        
        .anno-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            margin-bottom: 6px;
        }
        
        .anno-item {
            padding: 6px 8px;
            background: #f9f9f9;
            border-radius: 3px;
            border-left: 3px solid #2C5F2D;
            font-size: 11px;
            word-wrap: break-word;
            line-height: 1.3;
        }
        
        .error {
            color: red;
            text-align: center;
            padding: 20px;
        }
        
        @media print {
            body {
                background: white;
                padding: 0;
            }
            
            .page {
                width: 210mm;
                height: 297mm;
                margin: 0;
                box-shadow: none;
                page-break-after: always;
            }
        }
        
        @page {
            size: A4;
            margin: 0;
        }
    </style>
</head>
<body>
'''
    
    # 循环生成每一页
    success_count = 0
    fail_count = 0
    
    for idx, row in df_inconsistent.iterrows():
        image_id = row['图像ID']
        filename = row['文件名']
        
        print(f"处理 [{idx+1}/{len(df_inconsistent)}]: {filename} (ID: {image_id})")
        
        try:
            # 从数据库获取图片路径
            img_path = get_image_path(image_id, conn)
            
            if img_path and os.path.exists(img_path):
                # 转换为base64
                img_base64 = image_to_base64(img_path)
                if img_base64:
                    page_html = generate_html_page(row, img_base64)
                    html_content += page_html
                    success_count += 1
                    print(f"  ✅ 成功")
                else:
                    print(f"  ⚠️  图片读取失败")
                    fail_count += 1
            else:
                print(f"  ⚠️  图片不存在: {img_path}")
                fail_count += 1
                
        except Exception as e:
            print(f"  ❌ 错误: {e}")
            fail_count += 1
    
    # HTML尾部
    html_content += '''
</body>
</html>
'''
    
    # 写入文件
    with open(output_html, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    conn.close()
    
    print()
    print("="*80)
    print("✅ HTML文件已生成完成！")
    print("="*80)
    print(f"📄 输出文件: {output_html}")
    print(f"📊 成功: {success_count} 张")
    if fail_count > 0:
        print(f"⚠️  失败: {fail_count} 张")
    print(f"📏 每页尺寸: A4 (210mm × 297mm)")
    print(f"📄 总页数: {success_count} 页")
    print("="*80)
    print()
    print("💡 提示：")
    print("  - 在浏览器中打开HTML文件")
    print("  - 使用 Ctrl+P 或 Cmd+P 打印")
    print("  - 选择'另存为PDF'")
    print("  - 每张图片会严格控制在一页A4纸内")
    print()

if __name__ == '__main__':
    main()


