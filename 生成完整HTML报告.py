#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成任务4完整标注报告HTML（包含一致和不一致的所有50张图片）
每个PDF一张网页，控制不超出
"""

import pandas as pd
import base64
import os
import mysql.connector
import json
from datetime import datetime
from collections import defaultdict

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

def parse_annotation_data(data_str):
    """解析标注数据JSON字符串"""
    if pd.isna(data_str) or data_str == '':
        return None
    try:
        return json.loads(data_str)
    except:
        return None

def normalize_annotation(ann_data):
    """标准化标注数据"""
    if not ann_data:
        return None
    
    tubule_type = ann_data.get('小管1', '') or ann_data.get('小管', '')
    atrophy = ann_data.get('萎缩', '')
    
    parts = []
    if tubule_type:
        parts.append(f"小管类型:{tubule_type}")
    if atrophy:
        parts.append(f"萎缩:{atrophy}")
    
    return ' | '.join(parts) if parts else None

def get_image_path(image_id, conn):
    """从数据库获取图片路径"""
    cursor = conn.cursor(dictionary=True)
    query = "SELECT file_path, filename FROM images WHERE id = %s"
    cursor.execute(query, (image_id,))
    result = cursor.fetchone()
    cursor.close()
    
    if result:
        file_path = result['file_path']
        if file_path.startswith('/api/files/'):
            filename = file_path.replace('/api/files/', '')
            return os.path.join('/data/hong/tag/images', filename), result['filename']
    return None, None

def image_to_base64(image_path):
    """将图片转换为base64编码"""
    try:
        with open(image_path, 'rb') as f:
            return base64.b64encode(f.read()).decode()
    except Exception as e:
        print(f"  图片读取失败: {e}")
        return None

def generate_consistent_page(image_id, filename, img_base64, annotations):
    """生成一致图片的HTML页面"""
    
    if not img_base64:
        return f'<div class="error">图片加载失败: {filename}</div>'
    
    # 获取一致的标注内容
    teachers = sorted(annotations.keys())
    first_teacher = teachers[0]
    consistent_annotation = annotations[first_teacher]
    
    anno_text = f"【所有老师】: {consistent_annotation}"
    
    page_html = f'''
    <div class="page">
        <div class="title">{filename}</div>
        
        <div class="content-wrapper">
            <div class="image-container">
                <img src="data:image/jpeg;base64,{img_base64}" alt="{filename}">
            </div>
            
            <div class="info-section">
                <div class="info-box consistent">
                    <div class="box-title">✅ 标注一致</div>
                    <div class="box-content">
                        <div class="stats-row">
                            <span><strong>标注状态:</strong> 完全一致</span>
                            <span><strong>标注人数:</strong> {len(teachers)}</span>
                        </div>
                        <div class="consistent-info">
                            <strong>参与老师:</strong> {', '.join(teachers)}
                        </div>
                        <div class="anno-content">
                            <strong>一致的标注内容:</strong>
                            <div class="anno-item-single">{anno_text}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    '''
    
    return page_html

def generate_inconsistent_page(image_id, filename, img_base64, annotations):
    """生成不一致图片的HTML页面"""
    
    if not img_base64:
        return f'<div class="error">图片加载失败: {filename}</div>'
    
    # 按标注内容分组
    groups = defaultdict(list)
    for username, annotation in annotations.items():
        groups[annotation].append(username)
    
    # 生成分组详情和标注内容
    group_details = []
    anno_parts = []
    
    for annotation, usernames in sorted(groups.items(), key=lambda x: -len(x[1])):
        user_count = len(usernames)
        user_list = ', '.join(usernames)
        group_details.append(f"【{user_count}人】{user_list}")
        anno_parts.append(f"【{user_list}】: {annotation}")
    
    group_details_str = ' | '.join(group_details)
    
    # 将标注内容分成两列显示
    anno_grid_html = '<div class="anno-grid">'
    for i in range(0, len(anno_parts), 2):
        anno_grid_html += '<div class="anno-row">'
        anno_grid_html += f'<div class="anno-item">{anno_parts[i]}</div>'
        if i + 1 < len(anno_parts):
            anno_grid_html += f'<div class="anno-item">{anno_parts[i + 1]}</div>'
        else:
            anno_grid_html += '<div class="anno-item"></div>'
        anno_grid_html += '</div>'
    anno_grid_html += '</div>'
    
    page_html = f'''
    <div class="page">
        <div class="title">{filename}</div>
        
        <div class="content-wrapper">
            <div class="image-container">
                <img src="data:image/jpeg;base64,{img_base64}" alt="{filename}">
            </div>
            
            <div class="info-section">
                <div class="info-box">
                    <div class="box-title">⚠️ 标注不一致</div>
                    <div class="box-content">
                        <div class="stats-row">
                            <span><strong>不同标注类型数:</strong> {len(groups)}</span>
                            <span><strong>总标注人数:</strong> {len(annotations)}</span>
                        </div>
                        <div class="group-info"><strong>标注分组:</strong> {group_details_str}</div>
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
    print("📊 生成任务4完整标注报告HTML（所有50张图片）")
    print("="*80)
    print()
    
    # 连接数据库
    print("📊 连接数据库...")
    conn = get_db_connection()
    if not conn:
        return
    
    try:
        # 定义5位老师
        teachers = ['任雅丽', '喻小娟', '张旭', '王惠', '蒋镭']
        
        # 获取所有标注数据
        query = """
        SELECT 
            i.id as image_id,
            i.filename,
            u.username,
            a.labels as annotation_data
        FROM images i
        INNER JOIN image_assignments ia ON i.id = ia.image_id AND ia.project_id = 4
        INNER JOIN users u ON ia.user_id = u.id
        INNER JOIN annotations a ON ia.image_id = a.image_id AND ia.user_id = a.user_id
        WHERE u.username IN ('任雅丽', '喻小娟', '张旭', '王惠', '蒋镭')
        ORDER BY i.id, u.username
        """
        
        print("📋 正在获取标注数据...")
        df_annotations = pd.read_sql(query, conn)
        print(f"✅ 获取到 {len(df_annotations)} 条标注记录")
        print()
        
        # 按图片分组
        annotations_by_image = {}
        
        for _, row in df_annotations.iterrows():
            image_id = row['image_id']
            username = row['username']
            filename = row['filename']
            ann_data = parse_annotation_data(row['annotation_data'])
            
            if image_id not in annotations_by_image:
                annotations_by_image[image_id] = {
                    'filename': filename,
                    'annotations': {}
                }
            
            if ann_data:
                normalized = normalize_annotation(ann_data)
                annotations_by_image[image_id]['annotations'][username] = normalized
        
        # 分类为一致和不一致
        consistent_images = []
        inconsistent_images = []
        
        for image_id, data in sorted(annotations_by_image.items()):
            if len(data['annotations']) == 5:
                unique_annotations = set(data['annotations'].values())
                if len(unique_annotations) == 1:
                    consistent_images.append((image_id, data))
                else:
                    inconsistent_images.append((image_id, data))
        
        total_images = len(consistent_images) + len(inconsistent_images)
        
        print(f"📊 图片统计:")
        print(f"   ✅ 完全一致: {len(consistent_images)} 张")
        print(f"   ⚠️  存在不一致: {len(inconsistent_images)} 张")
        print(f"   📄 总计: {total_images} 张")
        print()
        
        # 创建输出目录
        output_dir = '/data/hong/tag/完整标注报告HTML'
        os.makedirs(output_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        output_html = os.path.join(output_dir, f'完整标注报告_{timestamp}.html')
        
        print(f"📝 开始生成HTML文件...")
        print(f"📄 输出文件: {output_html}")
        print()
        
        # HTML头部
        html_content = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>任务4完整标注报告（50张）</title>
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
        
        .info-box.consistent {
            border-color: #28a745;
            background: #f0f9f4;
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
        
        .info-box.consistent .box-title {
            color: #28a745;
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
        
        .group-info, .consistent-info {
            margin-bottom: 8px;
            line-height: 1.4;
            word-wrap: break-word;
        }
        
        .anno-title {
            margin-bottom: 6px;
        }
        
        .anno-content {
            margin-top: 10px;
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
        
        .anno-item-single {
            padding: 8px 12px;
            background: #e8f5e9;
            border-radius: 4px;
            border-left: 4px solid #28a745;
            font-size: 12px;
            margin-top: 6px;
            line-height: 1.4;
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
        
        # 先生成一致的图片
        success_count = 0
        fail_count = 0
        
        print("✅ 处理完全一致的图片...")
        for image_id, data in consistent_images:
            filename = data['filename']
            annotations = data['annotations']
            
            print(f"  [{success_count + fail_count + 1}/{total_images}] {filename} (一致)")
            
            try:
                img_path, _ = get_image_path(image_id, conn)
                
                if img_path and os.path.exists(img_path):
                    img_base64 = image_to_base64(img_path)
                    if img_base64:
                        page_html = generate_consistent_page(image_id, filename, img_base64, annotations)
                        html_content += page_html
                        success_count += 1
                    else:
                        fail_count += 1
                else:
                    fail_count += 1
                    
            except Exception as e:
                print(f"    ❌ 错误: {e}")
                fail_count += 1
        
        print()
        print("⚠️  处理存在不一致的图片...")
        for image_id, data in inconsistent_images:
            filename = data['filename']
            annotations = data['annotations']
            
            print(f"  [{success_count + fail_count + 1}/{total_images}] {filename} (不一致)")
            
            try:
                img_path, _ = get_image_path(image_id, conn)
                
                if img_path and os.path.exists(img_path):
                    img_base64 = image_to_base64(img_path)
                    if img_base64:
                        page_html = generate_inconsistent_page(image_id, filename, img_base64, annotations)
                        html_content += page_html
                        success_count += 1
                    else:
                        fail_count += 1
                else:
                    fail_count += 1
                    
            except Exception as e:
                print(f"    ❌ 错误: {e}")
                fail_count += 1
        
        # HTML尾部
        html_content += '''
</body>
</html>
'''
        
        # 写入文件
        with open(output_html, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
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
        print()
        print(f"📈 详细统计:")
        print(f"   ✅ 完全一致: {len(consistent_images)} 页")
        print(f"   ⚠️  存在不一致: {len(inconsistent_images)} 页")
        print("="*80)
        print()
        print("💡 提示：")
        print("  - 在浏览器中打开HTML文件")
        print("  - 使用 Ctrl+P 或 Cmd+P 打印")
        print("  - 选择'另存为PDF'")
        print("  - 每张图片会严格控制在一页A4纸内")
        print()
        
    finally:
        conn.close()

if __name__ == '__main__':
    main()


