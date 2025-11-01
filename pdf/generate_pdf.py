#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages
from matplotlib.patches import Rectangle, FancyBboxPatch
from PIL import Image
import os
import textwrap

# 设置macOS中文字体
plt.rcParams['font.sans-serif'] = ['Arial Unicode MS', 'Heiti TC', 'PingFang SC', 'STHeiti']
plt.rcParams['axes.unicode_minus'] = False

def wrap_text(text, width=80):
    """文本换行处理"""
    if not text or str(text) == 'nan':
        return ""
    text = str(text)
    lines = []
    for line in text.split('\n'):
        if line.strip():
            # 更激进的换行策略
            wrapped = textwrap.fill(line, width=width, break_long_words=True, break_on_hyphens=True)
            lines.append(wrapped)
        else:
            lines.append("")
    return '\n'.join(lines)

def get_pathology_info(filename, df_2023, df_2024):
    """根据文件名前10位匹配病理号"""
    # 提取文件名前缀（如 K2024-4538）并去掉K
    # 从 K2024-4538_xxx.jpg 提取 2024-4538
    prefix = filename[:10]  # K2024-4538
    if prefix.startswith('K'):
        prefix = prefix[1:]  # 去掉K，变成 2024-4538
    
    # 尝试在2023年数据中查找
    match_2023 = df_2023[df_2023['病理号'].astype(str).str.contains(prefix, na=False, regex=False)]
    if not match_2023.empty:
        return {
            '病理诊断': match_2023.iloc[0]['病理诊断'],
            'EM诊断': match_2023.iloc[0]['EM诊断']
        }
    
    # 尝试在2024年数据中查找
    match_2024 = df_2024[df_2024['病理号'].astype(str).str.contains(prefix, na=False, regex=False)]
    if not match_2024.empty:
        return {
            '病理诊断': match_2024.iloc[0]['病理诊断'],
            'EM诊断': match_2024.iloc[0]['EM诊断']
        }
    
    return None

def create_pdf_page(row, img_path, df_2023, df_2024, pdf):
    """为单张图片创建一页PDF"""
    filename = row['文件名']
    
    # 检查图片是否存在
    if not os.path.exists(img_path):
        print(f"图片不存在: {img_path}")
        return False
    
    try:
        # 创建figure，加大尺寸
        fig = plt.figure(figsize=(12, 17))
        
        # 1. 顶部：文件名（无框，更简洁）- 更紧凑
        ax_title = plt.subplot2grid((40, 1), (1, 0), rowspan=1)
        ax_title.axis('off')
        ax_title.text(0.5, 0.5, filename, 
                     ha='center', va='center', 
                     fontsize=18, fontweight='bold',
                     color='#1a1a1a')
        
        # 2. 图片（增大显示区域）- 紧挨着文件名
        ax_img = plt.subplot2grid((40, 1), (3, 0), rowspan=16)
        img = Image.open(img_path)
        ax_img.imshow(img)
        ax_img.axis('off')
        
        # 3. 标注详情区域 - 紧凑排列
        ax_anno = plt.subplot2grid((40, 1), (20, 0), rowspan=9)
        ax_anno.axis('off')
        ax_anno.set_xlim(0, 1)
        ax_anno.set_ylim(0, 1)
        
        # 解析各组标注内容，按2列排列
        anno_parts = row['各组标注内容'].split(' | ')
        
        # 构建标注详情文本
        anno_text = f"不同标注类型数: {row['不同标注类型数']}   |   总标注人数: {row['总标注人数']}\n\n"
        anno_text += f"标注分组:\n{wrap_text(row['标注分组详情'], width=40)}\n\n"
        anno_text += f"各组标注内容:\n"
        
        # 每行显示2个标注内容
        for i in range(0, len(anno_parts), 2):
            if i + 1 < len(anno_parts):
                # 两列显示
                left = anno_parts[i].strip()
                right = anno_parts[i + 1].strip()
                anno_text += f"{left}    |    {right}\n"
            else:
                # 只有一列
                anno_text += f"{anno_parts[i].strip()}\n"
        
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
        
        # 4. 病理诊断信息区域 - 紧凑排列
        ax_patho = plt.subplot2grid((40, 1), (30, 0), rowspan=9)
        ax_patho.axis('off')
        ax_patho.set_xlim(0, 1)
        ax_patho.set_ylim(0, 1)
        
        # 获取病理信息
        patho_info = get_pathology_info(filename, df_2023, df_2024)
        
        if patho_info:
            patho_diag = wrap_text(str(patho_info['病理诊断']), width=40)
            em_diag = wrap_text(str(patho_info['EM诊断']), width=40)
            
            # 如果EM诊断为空或nan，不显示
            if em_diag and em_diag.strip() and em_diag.strip().lower() != 'nan':
                patho_text = f"病理诊断:\n{patho_diag}\n\nEM诊断:\n{em_diag}"
            else:
                patho_text = f"病理诊断:\n{patho_diag}"
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
        return True
    except Exception as e:
        print(f"生成页面失败: {filename}, 错误: {e}")
        return False

def main():
    # 读取CSV文件
    print("读取CSV文件...")
    df_inconsistent = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/inconsistent_annotations.csv')
    df_2023 = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/2023年.csv')
    df_2024 = pd.read_csv('/Users/felix/Desktop/inconsistent_annotations/2024年.csv')
    
    # 创建输出目录
    output_dir = '/Users/felix/Desktop/inconsistent_annotations/output_pdfs'
    os.makedirs(output_dir, exist_ok=True)
    
    # 输出PDF文件路径
    output_pdf = os.path.join(output_dir, 'inconsistent_annotations_all.pdf')
    
    print(f"\n开始生成PDF文件...")
    print(f"输出文件: {output_pdf}")
    print(f"总共需要处理: {len(df_inconsistent)} 张图片\n")
    
    success_count = 0
    fail_count = 0
    
    # 创建一个大的PDF文件
    with PdfPages(output_pdf) as pdf:
        for idx, row in df_inconsistent.iterrows():
            filename = row['文件名']
            img_path = f'/Users/felix/Desktop/inconsistent_annotations/no_same/{filename}'
            
            print(f"处理 [{idx+1}/{len(df_inconsistent)}]: {filename}")
            
            if create_pdf_page(row, img_path, df_2023, df_2024, pdf):
                success_count += 1
            else:
                fail_count += 1
    
    print(f"\n{'='*60}")
    print(f"✅ 所有PDF已生成完成！")
    print(f"📄 输出文件: {output_pdf}")
    print(f"📊 成功: {success_count} 张")
    if fail_count > 0:
        print(f"❌ 失败: {fail_count} 张")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()

