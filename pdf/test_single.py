#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages
from matplotlib.patches import FancyBboxPatch
from PIL import Image
import textwrap

# 设置macOS中文字体
plt.rcParams['font.sans-serif'] = ['Arial Unicode MS', 'Heiti TC', 'PingFang SC', 'STHeiti']
plt.rcParams['axes.unicode_minus'] = False

def wrap_text(text, width=80):
    """文本换行处理"""
    if not text:
        return ""
    lines = []
    for line in text.split('\n'):
        if line:
            wrapped = textwrap.fill(line, width=width, break_long_words=False, break_on_hyphens=False)
            lines.append(wrapped)
        else:
            lines.append("")
    return '\n'.join(lines)

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

# 读取CSV文件
print("读取CSV文件...")
df_inconsistent = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/inconsistent_annotations.csv')
df_2023 = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/2023年.csv')
df_2024 = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/2024年.csv')

# 测试第一张图片
row = df_inconsistent.iloc[0]
filename = row['文件名']
img_path = f'/Users/felix/Desktop/inconsistent_annotations/no_same/{filename}'
output_pdf = f'/Users/felix/Desktop/inconsistent_annotations/output_pdfs/{filename.replace(".jpg", "")}.pdf'

print(f"生成测试PDF: {filename}")

with PdfPages(output_pdf) as pdf:
    fig = plt.figure(figsize=(12, 17))
    
    # 1. 文件名（无框）- 更紧凑
    ax_title = plt.subplot2grid((40, 1), (1, 0), rowspan=1)
    ax_title.axis('off')
    ax_title.text(0.5, 0.5, filename, 
                 ha='center', va='center', 
                 fontsize=18, fontweight='bold',
                 color='#1a1a1a')
    
    # 2. 图片 - 紧挨着文件名
    ax_img = plt.subplot2grid((40, 1), (3, 0), rowspan=16)
    img = Image.open(img_path)
    ax_img.imshow(img)
    ax_img.axis('off')
    
    # 3. 标注详情 - 紧凑排列
    ax_anno = plt.subplot2grid((40, 1), (20, 0), rowspan=9)
    ax_anno.axis('off')
    ax_anno.set_xlim(0, 1)
    ax_anno.set_ylim(0, 1)
    
    # 解析各组标注内容，按3行2列排列
    anno_parts = row['各组标注内容'].split(' | ')
    anno_grid = []
    for i in range(0, len(anno_parts), 2):
        row_text = anno_parts[i]
        if i + 1 < len(anno_parts):
            row_text += '    |    ' + anno_parts[i + 1]
        anno_grid.append(row_text)
    
    anno_text = f"不同标注类型数: {row['不同标注类型数']}   |   总标注人数: {row['总标注人数']}\n\n"
    anno_text += f"标注分组: {wrap_text(row['标注分组详情'], width=75)}\n\n"
    anno_text += f"各组标注内容:\n"
    for grid_row in anno_grid:
        anno_text += wrap_text(grid_row, width=75) + "\n"
    
    # 自适应框大小，文字居中
    fancy_box = FancyBboxPatch((0.02, 0.02), 0.96, 0.96,
                              boxstyle="round,pad=0.015",
                              edgecolor='#2C5F2D', 
                              facecolor='#FAFFFE',
                              linewidth=2.5,
                              transform=ax_anno.transAxes,
                              zorder=0)
    ax_anno.add_patch(fancy_box)
    
    # 标题在框内顶部
    ax_anno.text(0.05, 0.94, "标注详情", 
                ha='left', va='top',
                fontsize=16, fontweight='bold',
                transform=ax_anno.transAxes,
                color='#2C5F2D')
    
    # 内容居中显示
    ax_anno.text(0.05, 0.52, anno_text,
                ha='left', va='center',
                fontsize=13,
                transform=ax_anno.transAxes,
                linespacing=1.8)
    
    # 4. 病理诊断 - 紧凑排列
    ax_patho = plt.subplot2grid((40, 1), (30, 0), rowspan=9)
    ax_patho.axis('off')
    ax_patho.set_xlim(0, 1)
    ax_patho.set_ylim(0, 1)
    
    patho_info = get_pathology_info(filename, df_2023, df_2024)
    
    if patho_info:
        patho_text = f"病理诊断:\n{wrap_text(str(patho_info['病理诊断']), width=75)}\n\n"
        patho_text += f"EM诊断:\n{wrap_text(str(patho_info['EM诊断']), width=75)}"
    else:
        patho_text = "未找到匹配的病理诊断信息"
    
    # 自适应框大小，文字居中
    fancy_box2 = FancyBboxPatch((0.02, 0.02), 0.96, 0.96,
                               boxstyle="round,pad=0.015",
                               edgecolor='#2C5F2D',
                               facecolor='#FAFFFE',
                               linewidth=2.5,
                               transform=ax_patho.transAxes,
                               zorder=0)
    ax_patho.add_patch(fancy_box2)
    
    # 标题在框内顶部
    ax_patho.text(0.05, 0.94, "病理诊断", 
                 ha='left', va='top',
                 fontsize=16, fontweight='bold',
                 transform=ax_patho.transAxes,
                 color='#2C5F2D')
    
    # 内容居中显示
    ax_patho.text(0.05, 0.52, patho_text,
                 ha='left', va='center',
                 fontsize=13,
                 transform=ax_patho.transAxes,
                 linespacing=1.8)
    
    plt.subplots_adjust(top=0.98, bottom=0.02, left=0.05, right=0.95, hspace=0.3)
    pdf.savefig(fig, dpi=150, bbox_inches='tight')
    plt.close(fig)

print(f"已生成: {output_pdf}")
print("请打开PDF查看效果！")

