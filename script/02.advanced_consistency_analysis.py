#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
高级标注一致性分析 - Nature风格
包含多种高级统计指标和可视化方法
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import json
from sklearn.metrics import cohen_kappa_score, confusion_matrix, classification_report
from itertools import combinations
from scipy import stats
from collections import Counter
import warnings
warnings.filterwarnings('ignore')

# Nature风格配色
NATURE_COLORS = {
    'primary': ['#0C5DA5', '#00B945', '#FF9500', '#FF2C00', '#845B97', '#474747', '#9e9e9e'],
    'sequential': ['#E8F4F8', '#B8D4E8', '#88B4D8', '#5894C8', '#2874B8', '#0C5DA5'],
    'contrast': ['#0C5DA5', '#FF2C00', '#00B945', '#FF9500']
}

# 设置matplotlib样式
plt.style.use('seaborn-v0_8-whitegrid')
plt.rcParams['font.family'] = 'Arial'
plt.rcParams['font.size'] = 10
plt.rcParams['axes.linewidth'] = 1.2
plt.rcParams['axes.edgecolor'] = '#333333'

# 拼音映射
PINYIN_MAP = {
    '蒋镭': 'Jiang Lei', '喻小娟': 'Yu Xiaojuan', 
    '王惠': 'Wang Hui', '张旭': 'Zhang Xu',
    'PT': 'PT', 'DT': 'DT', 'TAL': 'TAL', 'CD': 'CD',
    '不确定（远端/集合管）': 'Uncertain(DT/CD)',
    '不确定（近远）': 'Uncertain(PT/DT)',
    '不确定（远曲/原直）': 'Uncertain(DCT/PST)',
    'Atr': 'Atr', '否': 'No', '是': 'Yes',
}

def translate_to_pinyin(text):
    return PINYIN_MAP.get(text, text)

# 读取数据
df = pd.read_csv('任务4_标注详细数据_20251026_004627.csv', encoding='utf-8')
target_users = ['蒋镭', '喻小娟', '王惠', '张旭']
target_users_pinyin = [translate_to_pinyin(u) for u in target_users]

df_filtered = df[(df['用户名'].isin(target_users)) & (df['标注状态'] == '已标注')].copy()

def parse_annotation(json_str):
    try:
        data = json.loads(json_str)
        return translate_to_pinyin(data.get('小管1', '')), translate_to_pinyin(data.get('萎缩', ''))
    except:
        return '', ''

df_filtered['小管类型'] = df_filtered['标注数据'].apply(lambda x: parse_annotation(x)[0])
df_filtered['萎缩状态'] = df_filtered['标注数据'].apply(lambda x: parse_annotation(x)[1])
df_filtered['用户名_pinyin'] = df_filtered['用户名'].apply(translate_to_pinyin)

pivot_tubule = df_filtered.pivot_table(index='图片ID', columns='用户名_pinyin', values='小管类型', aggfunc='first')
pivot_atrophy = df_filtered.pivot_table(index='图片ID', columns='用户名_pinyin', values='萎缩状态', aggfunc='first')

print("Advanced Consistency Analysis Started...")
print(f"Total annotators: {len(target_users)}")
print(f"Common images: {len(pivot_tubule.dropna())} (tubule), {len(pivot_atrophy.dropna())} (atrophy)")

# ====================
# 1. Fleiss' Kappa - 多标注者一致性
# ====================

def fleiss_kappa(matrix):
    """
    计算Fleiss' Kappa
    matrix: n_items x n_categories
    """
    n_items, n_categories = matrix.shape
    n_raters = matrix.sum(axis=1)[0]
    
    # 计算每个项目的一致性
    P_i = (np.sum(matrix ** 2, axis=1) - n_raters) / (n_raters * (n_raters - 1))
    P_bar = np.mean(P_i)
    
    # 计算每个类别的边际概率
    p_j = np.sum(matrix, axis=0) / (n_items * n_raters)
    P_e_bar = np.sum(p_j ** 2)
    
    # 计算Fleiss' Kappa
    kappa = (P_bar - P_e_bar) / (1 - P_e_bar)
    return kappa

def calculate_fleiss_kappa(pivot_data, annotators):
    """为数据计算Fleiss' Kappa"""
    # 只使用所有标注者都完成的样本
    complete_data = pivot_data[annotators].dropna()
    if len(complete_data) == 0:
        return 0, None
    
    # 获取所有类别
    all_labels = []
    for col in annotators:
        all_labels.extend(complete_data[col].unique())
    categories = sorted(set(all_labels))
    
    # 构建计数矩阵
    n_items = len(complete_data)
    n_categories = len(categories)
    matrix = np.zeros((n_items, n_categories))
    
    for i, (_, row) in enumerate(complete_data.iterrows()):
        for label in row:
            if label in categories:
                cat_idx = categories.index(label)
                matrix[i, cat_idx] += 1
    
    kappa = fleiss_kappa(matrix)
    return kappa, categories

# 计算Fleiss' Kappa
fleiss_kappa_tubule, tubule_categories = calculate_fleiss_kappa(pivot_tubule, target_users_pinyin)
fleiss_kappa_atrophy, atrophy_categories = calculate_fleiss_kappa(pivot_atrophy, target_users_pinyin)

print(f"\nFleiss' Kappa:")
print(f"  Tubule Type: {fleiss_kappa_tubule:.3f}")
print(f"  Atrophy Status: {fleiss_kappa_atrophy:.3f}")

# ====================
# 2. 难例分析 - 分歧最大的样本
# ====================

def find_disagreement_samples(pivot_data, annotators, top_n=10):
    """找出分歧最大的样本"""
    complete_data = pivot_data[annotators].dropna()
    disagreement_scores = []
    
    for idx, row in complete_data.iterrows():
        # 计算该样本的唯一标注数
        unique_labels = len(set(row))
        # 分歧分数：唯一标注数越多，分歧越大
        disagreement_scores.append({
            'image_id': idx,
            'n_unique': unique_labels,
            'labels': list(row)
        })
    
    # 按分歧程度排序
    disagreement_scores.sort(key=lambda x: x['n_unique'], reverse=True)
    return disagreement_scores[:top_n]

difficult_tubule = find_disagreement_samples(pivot_tubule, target_users_pinyin, 15)
difficult_atrophy = find_disagreement_samples(pivot_atrophy, target_users_pinyin, 15)

print(f"\nMost difficult samples (highest disagreement):")
print(f"  Tubule: {difficult_tubule[0]['n_unique']} different labels")
print(f"  Atrophy: {difficult_atrophy[0]['n_unique']} different labels")

# ====================
# 3. 标注者偏好雷达图
# ====================

fig, axes = plt.subplots(1, 2, figsize=(14, 6), subplot_kw=dict(projection='polar'))
fig.patch.set_facecolor('white')

# 小管类型雷达图
ax = axes[0]
categories_tubule = ['PT', 'DT', 'TAL', 'CD']
angles = np.linspace(0, 2 * np.pi, len(categories_tubule), endpoint=False).tolist()
angles += angles[:1]

for idx, (user, user_py) in enumerate(zip(target_users, target_users_pinyin)):
    user_data = df_filtered[df_filtered['用户名'] == user]
    counts = []
    for cat in categories_tubule:
        count = len(user_data[user_data['小管类型'] == cat])
        counts.append(count)
    counts += counts[:1]
    
    ax.plot(angles, counts, 'o-', linewidth=2, 
            label=user_py, color=NATURE_COLORS['primary'][idx])
    ax.fill(angles, counts, alpha=0.15, color=NATURE_COLORS['primary'][idx])

ax.set_xticks(angles[:-1])
ax.set_xticklabels(categories_tubule, fontsize=10)
ax.set_ylim(0, max([max(counts) for counts in [ax.get_lines()[i].get_ydata() for i in range(len(target_users))]]) * 1.1)
ax.set_title('Tubule Type - Annotator Preference', 
             fontsize=12, fontweight='bold', pad=20, family='Arial')
ax.legend(loc='upper right', bbox_to_anchor=(1.3, 1.1), fontsize=9)
ax.grid(True, linestyle='--', alpha=0.3)

# 萎缩状态雷达图
ax = axes[1]
categories_atrophy = ['No', 'Atr']
angles_atr = np.linspace(0, 2 * np.pi, len(categories_atrophy), endpoint=False).tolist()
angles_atr += angles_atr[:1]

for idx, (user, user_py) in enumerate(zip(target_users, target_users_pinyin)):
    user_data = df_filtered[df_filtered['用户名'] == user]
    counts = []
    for cat in categories_atrophy:
        count = len(user_data[user_data['萎缩状态'] == cat])
        counts.append(count)
    counts += counts[:1]
    
    ax.plot(angles_atr, counts, 'o-', linewidth=2,
            label=user_py, color=NATURE_COLORS['primary'][idx])
    ax.fill(angles_atr, counts, alpha=0.15, color=NATURE_COLORS['primary'][idx])

ax.set_xticks(angles_atr[:-1])
ax.set_xticklabels(categories_atrophy, fontsize=10)
ax.set_title('Atrophy Status - Annotator Preference', 
             fontsize=12, fontweight='bold', pad=20, family='Arial')
ax.legend(loc='upper right', bbox_to_anchor=(1.3, 1.1), fontsize=9)
ax.grid(True, linestyle='--', alpha=0.3)

plt.tight_layout()
plt.savefig('Nature_RadarChart_AnnotatorPreference.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_RadarChart_AnnotatorPreference.png")
plt.close()

# ====================
# 4. 标注者可靠性评分
# ====================

def calculate_annotator_reliability(pivot_data, annotators):
    """计算每个标注者的可靠性评分"""
    complete_data = pivot_data[annotators].dropna()
    reliability_scores = {}
    
    for annotator in annotators:
        # 与其他所有标注者的平均一致率
        agreements = []
        for other in annotators:
            if other != annotator:
                valid_samples = complete_data[[annotator, other]].dropna()
                if len(valid_samples) > 0:
                    agreement = np.mean(valid_samples[annotator] == valid_samples[other])
                    agreements.append(agreement)
        
        reliability_scores[annotator] = {
            'mean_agreement': np.mean(agreements) if agreements else 0,
            'std_agreement': np.std(agreements) if agreements else 0,
            'min_agreement': np.min(agreements) if agreements else 0,
            'max_agreement': np.max(agreements) if agreements else 0
        }
    
    return reliability_scores

reliability_tubule = calculate_annotator_reliability(pivot_tubule, target_users_pinyin)
reliability_atrophy = calculate_annotator_reliability(pivot_atrophy, target_users_pinyin)

# 可视化可靠性评分
fig, axes = plt.subplots(1, 2, figsize=(14, 5))
fig.patch.set_facecolor('white')

# 小管类型可靠性
ax = axes[0]
annotators = list(reliability_tubule.keys())
means = [reliability_tubule[a]['mean_agreement'] for a in annotators]
stds = [reliability_tubule[a]['std_agreement'] for a in annotators]

bars = ax.bar(range(len(annotators)), means, yerr=stds, 
               color=NATURE_COLORS['primary'][:len(annotators)],
               edgecolor='white', linewidth=1.5, alpha=0.85,
               capsize=5, error_kw={'linewidth': 2})
ax.set_xticks(range(len(annotators)))
ax.set_xticklabels(annotators, rotation=15, ha='right')
ax.set_ylabel('Mean Agreement with Others', fontsize=11, fontweight='bold')
ax.set_title('Tubule Type - Annotator Reliability Score', 
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.axhline(np.mean(means), color='red', linestyle='--', linewidth=1.5, alpha=0.5, label='Average')
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.set_ylim(0, 1)
ax.grid(axis='y', alpha=0.3, linestyle='--')
ax.legend(fontsize=9)

# 在柱子上标注数值
for i, (bar, mean, std) in enumerate(zip(bars, means, stds)):
    ax.text(bar.get_x() + bar.get_width()/2., mean + std + 0.02,
            f'{mean:.3f}', ha='center', va='bottom', fontsize=9, fontweight='bold')

# 萎缩状态可靠性
ax = axes[1]
means_atr = [reliability_atrophy[a]['mean_agreement'] for a in annotators]
stds_atr = [reliability_atrophy[a]['std_agreement'] for a in annotators]

bars = ax.bar(range(len(annotators)), means_atr, yerr=stds_atr,
               color=NATURE_COLORS['primary'][:len(annotators)],
               edgecolor='white', linewidth=1.5, alpha=0.85,
               capsize=5, error_kw={'linewidth': 2})
ax.set_xticks(range(len(annotators)))
ax.set_xticklabels(annotators, rotation=15, ha='right')
ax.set_ylabel('Mean Agreement with Others', fontsize=11, fontweight='bold')
ax.set_title('Atrophy Status - Annotator Reliability Score',
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.axhline(np.mean(means_atr), color='red', linestyle='--', linewidth=1.5, alpha=0.5, label='Average')
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.set_ylim(0, 1)
ax.grid(axis='y', alpha=0.3, linestyle='--')
ax.legend(fontsize=9)

for i, (bar, mean, std) in enumerate(zip(bars, means_atr, stds_atr)):
    ax.text(bar.get_x() + bar.get_width()/2., mean + std + 0.02,
            f'{mean:.3f}', ha='center', va='bottom', fontsize=9, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_ReliabilityScore.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_ReliabilityScore.png")
plt.close()

# ====================
# 5. 一致性分布箱线图
# ====================

def get_pairwise_agreements(pivot_data, annotators):
    """获取所有两两一致性"""
    agreements = []
    for ann1, ann2 in combinations(annotators, 2):
        valid_samples = pivot_data[[ann1, ann2]].dropna()
        for _, row in valid_samples.iterrows():
            agreements.append({
                'pair': f'{ann1} vs {ann2}',
                'agree': 1 if row[ann1] == row[ann2] else 0
            })
    return pd.DataFrame(agreements)

df_agreement_tubule = get_pairwise_agreements(pivot_tubule, target_users_pinyin)
df_agreement_atrophy = get_pairwise_agreements(pivot_atrophy, target_users_pinyin)

fig, axes = plt.subplots(1, 2, figsize=(16, 6))
fig.patch.set_facecolor('white')

# 小管类型箱线图
ax = axes[0]
pairs = df_agreement_tubule['pair'].unique()
data_to_plot = [df_agreement_tubule[df_agreement_tubule['pair'] == pair]['agree'].values 
                for pair in pairs]

bp = ax.boxplot(data_to_plot, labels=[p.replace(' vs ', '\nvs\n') for p in pairs],
                patch_artist=True, widths=0.6,
                boxprops=dict(facecolor=NATURE_COLORS['primary'][0], alpha=0.6, edgecolor='black', linewidth=1.2),
                medianprops=dict(color='red', linewidth=2),
                whiskerprops=dict(color='black', linewidth=1.2),
                capprops=dict(color='black', linewidth=1.2))

ax.set_ylabel('Agreement (0=Disagree, 1=Agree)', fontsize=11, fontweight='bold')
ax.set_title('Tubule Type - Agreement Distribution by Annotator Pairs',
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.set_ylim(-0.1, 1.1)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='y', alpha=0.3, linestyle='--')
plt.setp(ax.xaxis.get_majorticklabels(), fontsize=8)

# 萎缩状态箱线图
ax = axes[1]
data_to_plot_atr = [df_agreement_atrophy[df_agreement_atrophy['pair'] == pair]['agree'].values 
                    for pair in pairs]

bp = ax.boxplot(data_to_plot_atr, labels=[p.replace(' vs ', '\nvs\n') for p in pairs],
                patch_artist=True, widths=0.6,
                boxprops=dict(facecolor=NATURE_COLORS['primary'][1], alpha=0.6, edgecolor='black', linewidth=1.2),
                medianprops=dict(color='red', linewidth=2),
                whiskerprops=dict(color='black', linewidth=1.2),
                capprops=dict(color='black', linewidth=1.2))

ax.set_ylabel('Agreement (0=Disagree, 1=Agree)', fontsize=11, fontweight='bold')
ax.set_title('Atrophy Status - Agreement Distribution by Annotator Pairs',
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.set_ylim(-0.1, 1.1)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='y', alpha=0.3, linestyle='--')
plt.setp(ax.xaxis.get_majorticklabels(), fontsize=8)

plt.tight_layout()
plt.savefig('Nature_AgreementDistribution.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_AgreementDistribution.png")
plt.close()

# ====================
# 6. 难例可视化
# ====================

fig, axes = plt.subplots(1, 2, figsize=(16, 7))
fig.patch.set_facecolor('white')

# 小管类型难例
ax = axes[0]
top_difficult = difficult_tubule[:10]
y_pos = np.arange(len(top_difficult))
n_unique = [item['n_unique'] for item in top_difficult]
colors_grad = plt.cm.Reds(np.linspace(0.4, 0.9, len(top_difficult)))

bars = ax.barh(y_pos, n_unique, color=colors_grad, edgecolor='white', linewidth=1.5, alpha=0.9)
ax.set_yticks(y_pos)
ax.set_yticklabels([f"Image {item['image_id']}" for item in top_difficult], fontsize=9)
ax.set_xlabel('Number of Different Labels', fontsize=11, fontweight='bold')
ax.set_title('Tubule Type - Top 10 Most Difficult Samples',
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='x', alpha=0.3, linestyle='--')
ax.invert_yaxis()

for i, (bar, item) in enumerate(zip(bars, top_difficult)):
    width = bar.get_width()
    # 显示标注分布
    label_counts = Counter(item['labels'])
    label_str = ', '.join([f"{k}({v})" for k, v in label_counts.most_common()])
    ax.text(width + 0.1, bar.get_y() + bar.get_height()/2., 
            label_str, ha='left', va='center', fontsize=7)

# 萎缩状态难例
ax = axes[1]
top_difficult_atr = difficult_atrophy[:10]
y_pos = np.arange(len(top_difficult_atr))
n_unique_atr = [item['n_unique'] for item in top_difficult_atr]
colors_grad = plt.cm.Oranges(np.linspace(0.4, 0.9, len(top_difficult_atr)))

bars = ax.barh(y_pos, n_unique_atr, color=colors_grad, edgecolor='white', linewidth=1.5, alpha=0.9)
ax.set_yticks(y_pos)
ax.set_yticklabels([f"Image {item['image_id']}" for item in top_difficult_atr], fontsize=9)
ax.set_xlabel('Number of Different Labels', fontsize=11, fontweight='bold')
ax.set_title('Atrophy Status - Top 10 Most Difficult Samples',
             fontsize=12, fontweight='bold', family='Arial', pad=15)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.grid(axis='x', alpha=0.3, linestyle='--')
ax.invert_yaxis()

for i, (bar, item) in enumerate(zip(bars, top_difficult_atr)):
    width = bar.get_width()
    label_counts = Counter(item['labels'])
    label_str = ', '.join([f"{k}({v})" for k, v in label_counts.most_common()])
    ax.text(width + 0.1, bar.get_y() + bar.get_height()/2.,
            label_str, ha='left', va='center', fontsize=7)

plt.tight_layout()
plt.savefig('Nature_DifficultSamples.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_DifficultSamples.png")
plt.close()

# ====================
# 7. 综合统计报告
# ====================

report = []
report.append("=" * 80)
report.append("ADVANCED ANNOTATION CONSISTENCY ANALYSIS REPORT")
report.append("=" * 80)
report.append("")

report.append("I. Multi-Rater Agreement Metrics")
report.append("-" * 80)
report.append(f"Fleiss' Kappa (Tubule Type): {fleiss_kappa_tubule:.3f}")
report.append(f"Fleiss' Kappa (Atrophy Status): {fleiss_kappa_atrophy:.3f}")
report.append("")
report.append("Interpretation:")
report.append("  < 0.00   : Poor agreement")
report.append("  0.00-0.20: Slight agreement")
report.append("  0.21-0.40: Fair agreement")
report.append("  0.41-0.60: Moderate agreement")
report.append("  0.61-0.80: Substantial agreement")
report.append("  0.81-1.00: Almost perfect agreement")
report.append("")

report.append("II. Annotator Reliability Scores")
report.append("-" * 80)
report.append("Tubule Type:")
for annotator in target_users_pinyin:
    score = reliability_tubule[annotator]
    report.append(f"  {annotator}:")
    report.append(f"    Mean agreement: {score['mean_agreement']:.3f} ± {score['std_agreement']:.3f}")
    report.append(f"    Range: [{score['min_agreement']:.3f}, {score['max_agreement']:.3f}]")

report.append("\nAtrophy Status:")
for annotator in target_users_pinyin:
    score = reliability_atrophy[annotator]
    report.append(f"  {annotator}:")
    report.append(f"    Mean agreement: {score['mean_agreement']:.3f} ± {score['std_agreement']:.3f}")
    report.append(f"    Range: [{score['min_agreement']:.3f}, {score['max_agreement']:.3f}]")

report.append("")
report.append("III. Most Difficult Samples (Highest Disagreement)")
report.append("-" * 80)
report.append("Tubule Type - Top 5:")
for i, item in enumerate(difficult_tubule[:5], 1):
    label_counts = Counter(item['labels'])
    report.append(f"  {i}. Image {item['image_id']}: {item['n_unique']} different labels")
    report.append(f"     Distribution: {dict(label_counts)}")

report.append("\nAtrophy Status - Top 5:")
for i, item in enumerate(difficult_atrophy[:5], 1):
    label_counts = Counter(item['labels'])
    report.append(f"  {i}. Image {item['image_id']}: {item['n_unique']} different labels")
    report.append(f"     Distribution: {dict(label_counts)}")

report.append("")
report.append("=" * 80)
report.append("REPORT COMPLETED")
report.append("=" * 80)

report_text = "\n".join(report)
print("\n" + report_text)

with open('Nature_AdvancedReport.txt', 'w', encoding='utf-8') as f:
    f.write(report_text)
print("\n✓ Saved: Nature_AdvancedReport.txt")

# ====================
# 8. 创建综合指标摘要图
# ====================

fig = plt.figure(figsize=(16, 10))
fig.patch.set_facecolor('white')
gs = fig.add_gridspec(3, 3, hspace=0.35, wspace=0.35)

# 1. Fleiss' Kappa比较
ax1 = fig.add_subplot(gs[0, 0])
metrics = ['Tubule\nType', 'Atrophy\nStatus']
values = [fleiss_kappa_tubule, fleiss_kappa_atrophy]
colors = [NATURE_COLORS['primary'][0], NATURE_COLORS['primary'][1]]
bars = ax1.bar(metrics, values, color=colors, edgecolor='white', linewidth=2, alpha=0.85)
ax1.set_ylabel("Fleiss' Kappa", fontsize=10, fontweight='bold')
ax1.set_title("Multi-Rater Agreement", fontsize=11, fontweight='bold', family='Arial')
ax1.set_ylim(0, 1)
ax1.spines['top'].set_visible(False)
ax1.spines['right'].set_visible(False)
ax1.grid(axis='y', alpha=0.3, linestyle='--')
for bar, val in zip(bars, values):
    ax1.text(bar.get_x() + bar.get_width()/2., val + 0.03,
            f'{val:.3f}', ha='center', va='bottom', fontsize=10, fontweight='bold')

# 2. 可靠性得分雷达图
ax2 = fig.add_subplot(gs[0, 1:], projection='polar')
angles = np.linspace(0, 2 * np.pi, len(target_users_pinyin), endpoint=False).tolist()
angles += angles[:1]

tubule_scores = [reliability_tubule[a]['mean_agreement'] for a in target_users_pinyin]
tubule_scores += tubule_scores[:1]
atrophy_scores = [reliability_atrophy[a]['mean_agreement'] for a in target_users_pinyin]
atrophy_scores += atrophy_scores[:1]

ax2.plot(angles, tubule_scores, 'o-', linewidth=2, label='Tubule', color=NATURE_COLORS['primary'][0])
ax2.fill(angles, tubule_scores, alpha=0.15, color=NATURE_COLORS['primary'][0])
ax2.plot(angles, atrophy_scores, 'o-', linewidth=2, label='Atrophy', color=NATURE_COLORS['primary'][1])
ax2.fill(angles, atrophy_scores, alpha=0.15, color=NATURE_COLORS['primary'][1])

ax2.set_xticks(angles[:-1])
ax2.set_xticklabels(target_users_pinyin, fontsize=9)
ax2.set_ylim(0, 1)
ax2.set_title('Reliability Scores', fontsize=11, fontweight='bold', family='Arial', pad=20)
ax2.legend(loc='upper right', bbox_to_anchor=(1.3, 1.1), fontsize=9)
ax2.grid(True, linestyle='--', alpha=0.3)

# 3-6. 可靠性柱状图
for idx, (annotator, color) in enumerate(zip(target_users_pinyin, NATURE_COLORS['primary'])):
    row = 1 + idx // 2
    col = (idx % 2) * 2
    ax = fig.add_subplot(gs[row, col:col+1])
    
    tubule_score = reliability_tubule[annotator]['mean_agreement']
    atrophy_score = reliability_atrophy[annotator]['mean_agreement']
    
    x = ['Tubule', 'Atrophy']
    y = [tubule_score, atrophy_score]
    bars = ax.bar(x, y, color=[NATURE_COLORS['primary'][0], NATURE_COLORS['primary'][1]],
                   edgecolor='white', linewidth=1.5, alpha=0.85)
    
    ax.set_ylim(0, 1)
    ax.set_title(f'{annotator}', fontsize=10, fontweight='bold', family='Arial')
    ax.set_ylabel('Reliability', fontsize=9, fontweight='bold')
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    
    for bar, val in zip(bars, y):
        ax.text(bar.get_x() + bar.get_width()/2., val + 0.03,
                f'{val:.3f}', ha='center', va='bottom', fontsize=8, fontweight='bold')

# 7. 难度分布
ax7 = fig.add_subplot(gs[2, :])
tubule_difficulties = [item['n_unique'] for item in difficult_tubule]
atrophy_difficulties = [item['n_unique'] for item in difficult_atrophy]

bins = np.arange(1, 5, 1)
ax7.hist([tubule_difficulties, atrophy_difficulties], bins=bins, 
         label=['Tubule Type', 'Atrophy Status'],
         color=[NATURE_COLORS['primary'][0], NATURE_COLORS['primary'][1]],
         alpha=0.7, edgecolor='white', linewidth=1.5)
ax7.set_xlabel('Number of Different Labels', fontsize=10, fontweight='bold')
ax7.set_ylabel('Number of Samples', fontsize=10, fontweight='bold')
ax7.set_title('Sample Difficulty Distribution', fontsize=11, fontweight='bold', family='Arial')
ax7.legend(fontsize=9)
ax7.spines['top'].set_visible(False)
ax7.spines['right'].set_visible(False)
ax7.grid(axis='y', alpha=0.3, linestyle='--')

fig.suptitle('Advanced Consistency Metrics Dashboard', 
            fontsize=14, fontweight='bold', y=0.995, family='Arial')

plt.savefig('Nature_AdvancedMetrics.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_AdvancedMetrics.png")
plt.close()

print("\n" + "="*80)
print("Advanced analysis completed! Generated files:")
print("  1. Nature_RadarChart_AnnotatorPreference.png")
print("  2. Nature_ReliabilityScore.png")
print("  3. Nature_AgreementDistribution.png")
print("  4. Nature_DifficultSamples.png")
print("  5. Nature_AdvancedMetrics.png")
print("  6. Nature_AdvancedReport.txt")
print("="*80)

