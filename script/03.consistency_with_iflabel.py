#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
包含if_label的标注一致性分析 - Nature风格
将if_label作为第5个标注者进行分析
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import warnings
from sklearn.metrics import cohen_kappa_score, confusion_matrix
from itertools import combinations
warnings.filterwarnings('ignore')

# Nature风格配色
NATURE_COLORS = {
    'primary': ['#0C5DA5', '#00B945', '#FF9500', '#FF2C00', '#845B97', '#474747', '#9e9e9e'],
    'iflabel': '#8B4513',  # 为if_label使用特殊颜色（棕色）
}

# 设置matplotlib样式
plt.style.use('seaborn-v0_8-whitegrid')
plt.rcParams['font.family'] = 'Arial'
plt.rcParams['font.size'] = 10
plt.rcParams['axes.linewidth'] = 1.2

# 拼音映射
PINYIN_MAP = {
    '蒋镭': 'Jiang Lei', '喻小娟': 'Yu Xiaojuan', 
    '王惠': 'Wang Hui', '张旭': 'Zhang Xu',
    'PT': 'PT', 'DT': 'DT', 'TAL': 'TAL', 'CD': 'CD',
    'Thin': 'Thin',
    '不确定（远端/集合管）': 'Uncertain(DT/CD)',
    '不确定（近远）': 'Uncertain(PT/DT)',
    '不确定（远曲/原直）': 'Uncertain(DCT/PST)',
    'Atr': 'Atr', '否': 'No', '是': 'Yes',
}

def translate_to_pinyin(text):
    if pd.isna(text) or text == '':
        return ''
    return PINYIN_MAP.get(str(text), str(text))

# 读取CSV文件
df = pd.read_csv('任务4_图片及标注完整数据_20251026_014437.csv', encoding='utf-8')

print("="*80)
print("Annotation Consistency Analysis with IF_LABEL")
print("="*80)
print(f"Total samples: {len(df)}")

# 提取小管类型数据
tubule_data = pd.DataFrame({
    'image_id': df['id'],
    'IF_Label': df['if_label'].apply(translate_to_pinyin),
    'Jiang Lei': df['蒋镭_小管类型'].apply(translate_to_pinyin),
    'Yu Xiaojuan': df['喻小娟_小管类型'].apply(translate_to_pinyin),
    'Wang Hui': df['王惠_小管类型'].apply(translate_to_pinyin),
    'Zhang Xu': df['张旭_小管类型'].apply(translate_to_pinyin),
})

# 提取萎缩状态数据 - if_label没有萎缩判断，所以只分析人工标注者
atrophy_data = pd.DataFrame({
    'image_id': df['id'],
    'Jiang Lei': df['蒋镭_萎缩'].apply(translate_to_pinyin),
    'Yu Xiaojuan': df['喻小娟_萎缩'].apply(translate_to_pinyin),
    'Wang Hui': df['王惠_萎缩'].apply(translate_to_pinyin),
    'Zhang Xu': df['张旭_萎缩'].apply(translate_to_pinyin),
})

# 清理空值
tubule_data = tubule_data.replace('', np.nan)
atrophy_data = atrophy_data.replace('', np.nan)

# 设置索引
tubule_data = tubule_data.set_index('image_id')
atrophy_data = atrophy_data.set_index('image_id')

annotators_with_if = ['IF_Label', 'Jiang Lei', 'Yu Xiaojuan', 'Wang Hui', 'Zhang Xu']
annotators_human = ['Jiang Lei', 'Yu Xiaojuan', 'Wang Hui', 'Zhang Xu']

print(f"\nAnnotators (including IF_Label): {len(annotators_with_if)}")
print(f"Complete tubule annotations: {tubule_data.dropna().shape[0]}")
print(f"Complete atrophy annotations: {atrophy_data.dropna().shape[0]}")

# ====================
# 1. 计算一致性矩阵
# ====================

def calculate_agreement_matrix(data, annotators):
    """计算一致性和Kappa矩阵"""
    n = len(annotators)
    agreement_matrix = np.zeros((n, n))
    kappa_matrix = np.zeros((n, n))
    
    for i, ann1 in enumerate(annotators):
        for j, ann2 in enumerate(annotators):
            if i == j:
                agreement_matrix[i, j] = 1.0
                kappa_matrix[i, j] = 1.0
            else:
                valid_samples = data[[ann1, ann2]].dropna()
                if len(valid_samples) > 0:
                    labels1 = valid_samples[ann1].values
                    labels2 = valid_samples[ann2].values
                    
                    agreement = np.mean(labels1 == labels2)
                    agreement_matrix[i, j] = agreement
                    
                    try:
                        kappa = cohen_kappa_score(labels1, labels2)
                        kappa_matrix[i, j] = kappa
                    except:
                        kappa_matrix[i, j] = 0
    
    return agreement_matrix, kappa_matrix

# 计算小管类型一致性
agreement_tubule, kappa_tubule = calculate_agreement_matrix(tubule_data, annotators_with_if)

# 计算萎缩状态一致性（只有人工标注者）
agreement_atrophy, kappa_atrophy = calculate_agreement_matrix(atrophy_data, annotators_human)

print("\n" + "="*80)
print("Tubule Type - Agreement with IF_Label:")
print("="*80)
for i, ann in enumerate(annotators_with_if):
    if ann != 'IF_Label':
        print(f"  {ann} vs IF_Label: Agreement={agreement_tubule[0, i]:.3f}, Kappa={kappa_tubule[0, i]:.3f}")

print("\nTubule Type - Human Annotators Agreement:")
print("-"*80)
avg_agreement_human = np.mean([agreement_tubule[i, j] 
                               for i in range(1, 5) for j in range(1, 5) if i < j])
print(f"  Average agreement among humans: {avg_agreement_human:.3f}")

# ====================
# 2. 一致性热图 - 小管类型（包含IF_Label）
# ====================

fig, axes = plt.subplots(1, 2, figsize=(14, 6))
fig.patch.set_facecolor('white')

# 简单一致率热图
ax = axes[0]
sns.heatmap(agreement_tubule, annot=True, fmt='.3f', cmap='Blues',
            xticklabels=annotators_with_if, yticklabels=annotators_with_if,
            vmin=0.3, vmax=1.0, cbar_kws={'label': 'Agreement Rate'},
            ax=ax, linewidths=0.5, linecolor='white', square=True)
ax.set_title('Tubule Type - Agreement Rate (with IF_Label)', 
             fontsize=12, fontweight='bold', pad=12, family='Arial')
ax.set_xlabel('Annotator', fontsize=11, fontweight='bold')
ax.set_ylabel('Annotator', fontsize=11, fontweight='bold')

# 高亮IF_Label行和列
for i in range(len(annotators_with_if)):
    ax.add_patch(plt.Rectangle((0, 0), 5, 1, fill=False, edgecolor='red', lw=2))
    ax.add_patch(plt.Rectangle((0, 0), 1, 5, fill=False, edgecolor='red', lw=2))

# Kappa系数热图
ax = axes[1]
sns.heatmap(kappa_tubule, annot=True, fmt='.3f', cmap='RdYlGn',
            xticklabels=annotators_with_if, yticklabels=annotators_with_if,
            vmin=0, vmax=1, cbar_kws={'label': "Cohen's Kappa"},
            ax=ax, linewidths=0.5, linecolor='white', square=True)
ax.set_title("Tubule Type - Cohen's Kappa (with IF_Label)", 
             fontsize=12, fontweight='bold', pad=12, family='Arial')
ax.set_xlabel('Annotator', fontsize=11, fontweight='bold')
ax.set_ylabel('Annotator', fontsize=11, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_Consistency_WithIFLabel.png', dpi=300, bbox_inches='tight', facecolor='white')
print("\n✓ Saved: Nature_Consistency_WithIFLabel.png")
plt.close()

# ====================
# 3. IF_Label vs 人工标注者对比图
# ====================

fig, axes = plt.subplots(2, 2, figsize=(14, 12))
fig.patch.set_facecolor('white')

# 为每个人工标注者创建与IF_Label的混淆矩阵
for idx, annotator in enumerate(annotators_human):
    ax = axes[idx // 2, idx % 2]
    
    valid_samples = tubule_data[['IF_Label', annotator]].dropna()
    
    if len(valid_samples) > 0:
        labels_if = valid_samples['IF_Label'].values
        labels_human = valid_samples[annotator].values
        
        # 获取所有类别
        all_labels = sorted(set(labels_if) | set(labels_human))
        
        # 计算混淆矩阵
        cm = confusion_matrix(labels_if, labels_human, labels=all_labels)
        
        # 归一化
        cm_normalized = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis] * 100
        
        # 绘制热图
        sns.heatmap(cm_normalized, annot=cm, fmt='d', cmap='Blues',
                   xticklabels=all_labels, yticklabels=all_labels,
                   cbar_kws={'label': 'Percentage (%)'}, ax=ax,
                   linewidths=0.5, linecolor='white', vmin=0, vmax=100)
        
        # 计算指标
        agreement = np.mean(labels_if == labels_human)
        kappa = cohen_kappa_score(labels_if, labels_human)
        
        ax.set_title(f'IF_Label vs {annotator}\nAgreement: {agreement:.3f}, κ: {kappa:.3f}',
                    fontsize=11, fontweight='bold', family='Arial')
        ax.set_xlabel(f'{annotator} annotation', fontsize=10, fontweight='bold')
        ax.set_ylabel('IF_Label prediction', fontsize=10, fontweight='bold')
        plt.setp(ax.xaxis.get_majorticklabels(), rotation=45, ha='right', fontsize=8)
        plt.setp(ax.yaxis.get_majorticklabels(), fontsize=8)

plt.tight_layout()
plt.savefig('Nature_IFLabel_vs_Human.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_IFLabel_vs_Human.png")
plt.close()

# ====================
# 4. 一致性对比条形图
# ====================

fig, axes = plt.subplots(1, 2, figsize=(14, 6))
fig.patch.set_facecolor('white')

# IF_Label与每个人工标注者的一致性
ax = axes[0]
human_ann = annotators_human
agreements_with_if = [agreement_tubule[0, i+1] for i in range(4)]
kappas_with_if = [kappa_tubule[0, i+1] for i in range(4)]

x = np.arange(len(human_ann))
width = 0.35

bars1 = ax.bar(x - width/2, agreements_with_if, width, 
               label='Agreement Rate', color=NATURE_COLORS['primary'][0],
               edgecolor='white', linewidth=1.5, alpha=0.85)
bars2 = ax.bar(x + width/2, kappas_with_if, width,
               label="Cohen's Kappa", color=NATURE_COLORS['primary'][1],
               edgecolor='white', linewidth=1.5, alpha=0.85)

ax.set_ylabel('Score', fontsize=11, fontweight='bold')
ax.set_title('IF_Label Agreement with Human Annotators', 
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(human_ann, rotation=15, ha='right')
ax.legend(fontsize=10)
ax.set_ylim(0, 1)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='y', alpha=0.3, linestyle='--')

# 添加数值标签
for bars in [bars1, bars2]:
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height + 0.02,
                f'{height:.3f}', ha='center', va='bottom', fontsize=8, fontweight='bold')

# 人工标注者之间的平均一致性 vs IF_Label
ax = axes[1]
categories = ['IF_Label vs\nHumans', 'Among\nHumans']

avg_agreement_if = np.mean(agreements_with_if)
avg_kappa_if = np.mean(kappas_with_if)

agreement_scores = [avg_agreement_if, avg_agreement_human]
kappa_scores = [avg_kappa_if, np.mean([kappa_tubule[i, j] 
                                       for i in range(1, 5) for j in range(1, 5) if i < j])]

x = np.arange(len(categories))
bars1 = ax.bar(x - width/2, agreement_scores, width,
               label='Agreement Rate', color=NATURE_COLORS['primary'][0],
               edgecolor='white', linewidth=1.5, alpha=0.85)
bars2 = ax.bar(x + width/2, kappa_scores, width,
               label="Cohen's Kappa", color=NATURE_COLORS['primary'][1],
               edgecolor='white', linewidth=1.5, alpha=0.85)

ax.set_ylabel('Score', fontsize=11, fontweight='bold')
ax.set_title('Average Consistency Comparison', 
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(categories, fontsize=10)
ax.legend(fontsize=10)
ax.set_ylim(0, 1)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='y', alpha=0.3, linestyle='--')

for bars in [bars1, bars2]:
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height + 0.02,
                f'{height:.3f}', ha='center', va='bottom', fontsize=9, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_IFLabel_Comparison.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_IFLabel_Comparison.png")
plt.close()

# ====================
# 5. 类别级别的性能分析
# ====================

def calculate_category_metrics(data, if_col, human_cols):
    """计算每个类别的精确率、召回率和F1"""
    results = []
    
    for category in data[if_col].dropna().unique():
        if category == '':
            continue
            
        for human_col in human_cols:
            valid_samples = data[[if_col, human_col]].dropna()
            
            if len(valid_samples) == 0:
                continue
            
            # True Positives: IF_Label和人工都标注为该类别
            tp = np.sum((valid_samples[if_col] == category) & 
                       (valid_samples[human_col] == category))
            
            # False Positives: IF_Label标注为该类别，人工标注不是
            fp = np.sum((valid_samples[if_col] == category) & 
                       (valid_samples[human_col] != category))
            
            # False Negatives: IF_Label标注不是该类别，人工标注是
            fn = np.sum((valid_samples[if_col] != category) & 
                       (valid_samples[human_col] == category))
            
            # 计算指标
            precision = tp / (tp + fp) if (tp + fp) > 0 else 0
            recall = tp / (tp + fn) if (tp + fn) > 0 else 0
            f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0
            
            results.append({
                'Category': category,
                'Annotator': human_col,
                'Precision': precision,
                'Recall': recall,
                'F1': f1,
                'TP': tp,
                'FP': fp,
                'FN': fn
            })
    
    return pd.DataFrame(results)

category_metrics = calculate_category_metrics(tubule_data, 'IF_Label', annotators_human)

# 按类别汇总
category_summary = category_metrics.groupby('Category').agg({
    'Precision': 'mean',
    'Recall': 'mean',
    'F1': 'mean',
    'TP': 'sum',
    'FP': 'sum',
    'FN': 'sum'
}).reset_index()

# 可视化类别级别性能
fig, ax = plt.subplots(figsize=(12, 6))
fig.patch.set_facecolor('white')

categories = category_summary['Category'].values
x = np.arange(len(categories))
width = 0.25

bars1 = ax.bar(x - width, category_summary['Precision'], width,
               label='Precision', color=NATURE_COLORS['primary'][0],
               edgecolor='white', linewidth=1.5, alpha=0.85)
bars2 = ax.bar(x, category_summary['Recall'], width,
               label='Recall', color=NATURE_COLORS['primary'][1],
               edgecolor='white', linewidth=1.5, alpha=0.85)
bars3 = ax.bar(x + width, category_summary['F1'], width,
               label='F1 Score', color=NATURE_COLORS['primary'][2],
               edgecolor='white', linewidth=1.5, alpha=0.85)

ax.set_ylabel('Score', fontsize=11, fontweight='bold')
ax.set_title('IF_Label Performance by Tubule Category', 
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(categories, rotation=45, ha='right', fontsize=9)
ax.legend(fontsize=10)
ax.set_ylim(0, 1.1)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='y', alpha=0.3, linestyle='--')

# 添加数值标签
for bars in [bars1, bars2, bars3]:
    for bar in bars:
        height = bar.get_height()
        if height > 0:
            ax.text(bar.get_x() + bar.get_width()/2., height + 0.02,
                    f'{height:.2f}', ha='center', va='bottom', fontsize=7)

plt.tight_layout()
plt.savefig('Nature_IFLabel_CategoryPerformance.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_IFLabel_CategoryPerformance.png")
plt.close()

# ====================
# 6. 生成详细报告
# ====================

report = []
report.append("=" * 80)
report.append("ANNOTATION CONSISTENCY ANALYSIS WITH IF_LABEL")
report.append("=" * 80)
report.append("")

report.append("I. Basic Statistics")
report.append("-" * 80)
report.append(f"Total samples: {len(df)}")
report.append(f"Complete tubule annotations (all 5): {tubule_data.dropna().shape[0]}")
report.append(f"Complete atrophy annotations (humans only): {atrophy_data.dropna().shape[0]}")
report.append("")

report.append("II. IF_Label Agreement with Human Annotators (Tubule Type)")
report.append("-" * 80)
for i, ann in enumerate(annotators_human):
    report.append(f"{ann}:")
    report.append(f"  Agreement: {agreement_tubule[0, i+1]:.3f}")
    report.append(f"  Cohen's Kappa: {kappa_tubule[0, i+1]:.3f}")
report.append("")
report.append(f"Average Agreement (IF_Label vs Humans): {np.mean(agreements_with_if):.3f}")
report.append(f"Average Kappa (IF_Label vs Humans): {np.mean(kappas_with_if):.3f}")
report.append("")

report.append("III. Inter-Human Agreement (Tubule Type)")
report.append("-" * 80)
report.append(f"Average Agreement (Among Humans): {avg_agreement_human:.3f}")
avg_kappa_human = np.mean([kappa_tubule[i, j] 
                           for i in range(1, 5) for j in range(1, 5) if i < j])
report.append(f"Average Kappa (Among Humans): {avg_kappa_human:.3f}")
report.append("")

report.append("IV. IF_Label Performance by Category")
report.append("-" * 80)
report.append(f"{'Category':<20} {'Precision':<12} {'Recall':<12} {'F1':<12}")
report.append("-" * 80)
for _, row in category_summary.iterrows():
    report.append(f"{row['Category']:<20} {row['Precision']:<12.3f} "
                 f"{row['Recall']:<12.3f} {row['F1']:<12.3f}")
report.append("")

report.append("V. Key Findings")
report.append("-" * 80)

# 找出IF_Label表现最好和最差的类别
best_category = category_summary.loc[category_summary['F1'].idxmax()]
worst_category = category_summary.loc[category_summary['F1'].idxmin()]

report.append(f"Best performing category: {best_category['Category']} (F1={best_category['F1']:.3f})")
report.append(f"Worst performing category: {worst_category['Category']} (F1={worst_category['F1']:.3f})")
report.append("")

# 比较IF_Label和人工标注者
if np.mean(agreements_with_if) > avg_agreement_human:
    report.append("IF_Label shows HIGHER agreement with individual humans than")
    report.append("humans agree with each other, suggesting good model performance.")
else:
    report.append("Human annotators show HIGHER agreement among themselves than")
    report.append("with IF_Label, suggesting room for model improvement.")

report.append("")
report.append("=" * 80)
report.append("REPORT COMPLETED")
report.append("=" * 80)

report_text = "\n".join(report)
print("\n" + report_text)

with open('Nature_IFLabel_Report.txt', 'w', encoding='utf-8') as f:
    f.write(report_text)
print("\n✓ Saved: Nature_IFLabel_Report.txt")

print("\n" + "="*80)
print("Analysis completed! Generated files:")
print("  1. Nature_Consistency_WithIFLabel.png - Consistency heatmaps")
print("  2. Nature_IFLabel_vs_Human.png - Confusion matrices")
print("  3. Nature_IFLabel_Comparison.png - Comparison bar charts")
print("  4. Nature_IFLabel_CategoryPerformance.png - Category-level performance")
print("  5. Nature_IFLabel_Report.txt - Detailed report")
print("="*80)

