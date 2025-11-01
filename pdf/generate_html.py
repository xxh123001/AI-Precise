#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pandas as pd
import base64
import os

def get_pathology_info(filename, df_2023, df_2024):
    """根据文件名前10位匹配病理号"""
    prefix = filename[:10]
    if prefix.startswith('K'):
        prefix = prefix[1:]
    
    match_2023 = df_2023[df_2023['病理号'].astype(str).str.contains(prefix, na=False, regex=False)]
    if not match_2023.empty:
        return {
            '病理诊断': match_2023.iloc[0]['病理诊断'],
            'EM诊断': match_2023.iloc[0]['EM诊断']
        }
    
    match_2024 = df_2024[df_2024['病理号'].astype(str).str.contains(prefix, na=False, regex=False)]
    if not match_2024.empty:
        return {
            '病理诊断': match_2024.iloc[0]['病理诊断'],
            'EM诊断': match_2024.iloc[0]['EM诊断']
        }
    
    return None

def image_to_base64(image_path):
    """将图片转换为base64编码"""
    with open(image_path, 'rb') as f:
        return base64.b64encode(f.read()).decode()

def generate_html_page(row, img_path, df_2023, df_2024):
    """生成单页HTML内容"""
    filename = row['文件名']
    
    # 检查图片是否存在
    if not os.path.exists(img_path):
        return f'<div class="error">图片不存在: {filename}</div>'
    
    # 转换图片为base64
    img_base64 = image_to_base64(img_path)
    
    # 解析各组标注内容
    anno_parts = row['各组标注内容'].split(' | ')
    anno_grid_html = '<div class="anno-grid">'
    for i in range(0, len(anno_parts), 2):
        anno_grid_html += '<div class="anno-row">'
        anno_grid_html += f'<div class="anno-item">{anno_parts[i]}</div>'
        if i + 1 < len(anno_parts):
            anno_grid_html += f'<div class="anno-item">{anno_parts[i + 1]}</div>'
        anno_grid_html += '</div>'
    anno_grid_html += '</div>'
    
    # 获取病理信息
    patho_info = get_pathology_info(filename, df_2023, df_2024)
    if patho_info:
        patho_diag = str(patho_info['病理诊断'])
        em_diag = str(patho_info['EM诊断'])
        
        patho_html = f'<div><strong>病理诊断:</strong><br>{patho_diag}</div>'
        if em_diag and em_diag.strip() and em_diag.strip().lower() != 'nan':
            patho_html += f'<div style="margin-top: 15px;"><strong>EM诊断:</strong><br>{em_diag}</div>'
    else:
        patho_html = '<div>未找到匹配的病理诊断信息</div>'
    
    # 生成单页HTML
    page_html = f'''
    <div class="page">
        <div class="title">{filename}</div>
        
        <div class="image-container">
            <img src="data:image/jpeg;base64,{img_base64}" alt="{filename}">
        </div>
        
        <div class="info-box">
            <div class="box-title">标注详情</div>
            <div class="box-content">
                <div><strong>不同标注类型数:</strong> {row['不同标注类型数']} &nbsp;&nbsp;|&nbsp;&nbsp; <strong>总标注人数:</strong> {row['总标注人数']}</div>
                <div style="margin-top: 10px;"><strong>标注分组:</strong><br>{row['标注分组详情']}</div>
                <div style="margin-top: 10px;"><strong>各组标注内容:</strong></div>
                {anno_grid_html}
            </div>
        </div>
        
        <div class="info-box">
            <div class="box-title">病理诊断</div>
            <div class="box-content">
                {patho_html}
            </div>
        </div>
    </div>
    '''
    
    return page_html

def main():
    # 读取CSV文件
    print("读取CSV文件...")
    df_inconsistent = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/inconsistent_annotations.csv')
    df_2023 = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/2023年.csv')
    df_2024 = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/2024年.csv')
    
    # 创建输出目录
    output_dir = '/Users/felix/Desktop/inconsistent_annotations/output_pdfs'
    os.makedirs(output_dir, exist_ok=True)
    
    output_html = os.path.join(output_dir, 'inconsistent_annotations_all.html')
    
    print(f"\n开始生成HTML文件...")
    print(f"输出文件: {output_html}")
    print(f"总共需要处理: {len(df_inconsistent)} 张图片\n")
    
    # HTML头部
    html_content = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>不一致标注报告</title>
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
            padding: 20px;
            line-height: 1.6;
        }
        
        .page {
            max-width: 1200px;
            margin: 0 auto 40px;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            page-break-after: always;
        }
        
        .title {
            text-align: center;
            font-size: 20px;
            font-weight: bold;
            color: #1a1a1a;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 2px solid #e0e0e0;
        }
        
        .image-container {
            text-align: center;
            margin: 20px 0;
            background: #fafafa;
            padding: 20px;
            border-radius: 8px;
        }
        
        .image-container img {
            max-width: 100%;
            height: auto;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        
        .info-box {
            margin: 20px 0;
            border: 2.5px solid #2C5F2D;
            border-radius: 8px;
            background: #FAFFFE;
            padding: 20px;
        }
        
        .box-title {
            font-size: 18px;
            font-weight: bold;
            color: #2C5F2D;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .box-content {
            font-size: 14px;
            color: #333;
            word-wrap: break-word;
            word-break: break-all;
        }
        
        .anno-grid {
            margin-top: 10px;
        }
        
        .anno-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin-bottom: 8px;
        }
        
        .anno-item {
            padding: 8px 12px;
            background: #f9f9f9;
            border-radius: 4px;
            border-left: 3px solid #2C5F2D;
            font-size: 13px;
            word-wrap: break-word;
            word-break: break-all;
        }
        
        @media print {
            .page {
                box-shadow: none;
                margin-bottom: 0;
            }
        }
        
        @media (max-width: 768px) {
            .anno-row {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
'''
    
    # 循环生成每一页
    success_count = 0
    fail_count = 0
    
    for idx, row in df_inconsistent.iterrows():
        filename = row['文件名']
        img_path = f'/Users/felix/Desktop/inconsistent_annotations/no_same/{filename}'
        
        print(f"处理 [{idx+1}/{len(df_inconsistent)}]: {filename}")
        
        try:
            page_html = generate_html_page(row, img_path, df_2023, df_2024)
            html_content += page_html
            success_count += 1
        except Exception as e:
            print(f"  错误: {e}")
            fail_count += 1
    
    # HTML尾部
    html_content += '''
</body>
</html>
'''
    
    # 写入文件
    with open(output_html, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"\n{'='*60}")
    print(f"✅ HTML文件已生成完成！")
    print(f"📄 输出文件: {output_html}")
    print(f"📊 成功: {success_count} 张")
    if fail_count > 0:
        print(f"❌ 失败: {fail_count} 张")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()

