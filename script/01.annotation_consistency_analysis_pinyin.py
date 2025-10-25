#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
标注一致性分析脚本 - 拼音版本 Nature风格
分析4位标注者（蒋镭、喻小娟、王惠、张旭）之间的标注一致性
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from matplotlib import font_manager
import json
from sklearn.metrics import cohen_kappa_score, confusion_matrix
from itertools import combinations
import warnings
warnings.filterwarnings('ignore')

# Nature风格配色
NATURE_COLORS = {
    'primary': ['#0C5DA5', '#00B945', '#FF9500', '#FF2C00', '#845B97', '#474747', '#9e9e9e'],
    'heatmap': 'RdYlBu_r',
    'sequential': 'Blues',
    'diverging': 'RdBu_r'
}

# 设置matplotlib样式
plt.style.use('seaborn-v0_8-darkgrid')
plt.rcParams['font.family'] = 'Arial'
plt.rcParams['font.size'] = 10
plt.rcParams['axes.linewidth'] = 1.2
plt.rcParams['axes.edgecolor'] = '#333333'
plt.rcParams['axes.labelsize'] = 11
plt.rcParams['xtick.labelsize'] = 9
plt.rcParams['ytick.labelsize'] = 9
plt.rcParams['legend.fontsize'] = 9
plt.rcParams['figure.dpi'] = 100

# 拼音映射字典
PINYIN_MAP = {
    # 用户名
    '蒋镭': 'Jiang Lei',
    '喻小娟': 'Yu Xiaojuan',
    '王惠': 'Wang Hui',
    '张旭': 'Zhang Xu',
    
    # 小管类型
    'PT': 'PT',
    'DT': 'DT',
    'TAL': 'TAL',
    'CD': 'CD',
    '不确定（远端/集合管）': 'Uncertain(DT/CD)',
    '不确定（近远）': 'Uncertain(PT/DT)',
    '不确定（远曲/原直）': 'Uncertain(DCT/PST)',
    '不确定': 'Uncertain',
    
    # 萎缩状态
    'Atr': 'Atr',
    '否': 'No',
    '是': 'Yes',
}

def translate_to_pinyin(text):
    """将中文转换为拼音"""
    return PINYIN_MAP.get(text, text)

# 读取CSV文件
df = pd.read_csv('任务4_标注详细数据_20251026_004627.csv', encoding='utf-8')

# 指定的4位标注者
target_users = ['蒋镭', '喻小娟', '王惠', '张旭']
target_users_pinyin = [translate_to_pinyin(u) for u in target_users]

# 筛选目标用户并只保留已完成的标注
df_filtered = df[(df['用户名'].isin(target_users)) & (df['标注状态'] == '已标注')].copy()

print(f"Data loaded successfully!")
print(f"Total records: {len(df_filtered)}")
print(f"Completion status by annotator:")
for user, user_py in zip(target_users, target_users_pinyin):
    count = len(df_filtered[df_filtered['用户名'] == user])
    print(f"  {user_py}: {count} images")

# 解析标注数据JSON
def parse_annotation(json_str):
    """解析标注数据JSON字符串"""
    try:
        data = json.loads(json_str)
        tubule = translate_to_pinyin(data.get('小管1', ''))
        atrophy = translate_to_pinyin(data.get('萎缩', ''))
        return tubule, atrophy
    except:
        return '', ''

# 提取标注信息
df_filtered['小管类型'] = df_filtered['标注数据'].apply(lambda x: parse_annotation(x)[0])
df_filtered['萎缩状态'] = df_filtered['标注数据'].apply(lambda x: parse_annotation(x)[1])
df_filtered['用户名_pinyin'] = df_filtered['用户名'].apply(translate_to_pinyin)

# 创建透视表：每个图片ID对应每个用户的标注
pivot_tubule = df_filtered.pivot_table(
    index='图片ID', 
    columns='用户名_pinyin', 
    values='小管类型', 
    aggfunc='first'
)

pivot_atrophy = df_filtered.pivot_table(
    index='图片ID', 
    columns='用户名_pinyin', 
    values='萎缩状态', 
    aggfunc='first'
)

# 只保留所有4位标注者都完成的图片
common_images_tubule = pivot_tubule.dropna().index
common_images_atrophy = pivot_atrophy.dropna().index

print(f"\nCommon images annotated by all 4 annotators:")
print(f"  Tubule type: {len(common_images_tubule)} images")
print(f"  Atrophy status: {len(common_images_atrophy)} images")

# ====================
# 1. 计算两两标注者之间的一致性
# ====================

def calculate_agreement(data, annotators):
    """计算标注者之间的一致性矩阵"""
    n = len(annotators)
    agreement_matrix = np.zeros((n, n))
    kappa_matrix = np.zeros((n, n))
    
    for i, ann1 in enumerate(annotators):
        for j, ann2 in enumerate(annotators):
            if i == j:
                agreement_matrix[i, j] = 1.0
                kappa_matrix[i, j] = 1.0
            else:
                # 找到两个标注者都完成的样本
                valid_samples = data[[ann1, ann2]].dropna()
                if len(valid_samples) > 0:
                    labels1 = valid_samples[ann1].values
                    labels2 = valid_samples[ann2].values
                    
                    # 计算简单一致率
                    agreement = np.mean(labels1 == labels2)
                    agreement_matrix[i, j] = agreement
                    
                    # 计算Kappa系数
                    try:
                        kappa = cohen_kappa_score(labels1, labels2)
                        kappa_matrix[i, j] = kappa
                    except:
                        kappa_matrix[i, j] = 0
    
    return agreement_matrix, kappa_matrix

# 计算小管类型的一致性
agreement_tubule, kappa_tubule = calculate_agreement(
    pivot_tubule[target_users_pinyin], target_users_pinyin
)

# 计算萎缩状态的一致性
agreement_atrophy, kappa_atrophy = calculate_agreement(
    pivot_atrophy[target_users_pinyin], target_users_pinyin
)

# ====================
# 2. 创建可视化 - Nature风格
# ====================

# 图1: 小管类型一致性热图
fig, axes = plt.subplots(1, 2, figsize=(14, 5.5))
fig.patch.set_facecolor('white')

# 简单一致率热图
mask_diag = np.eye(len(target_users_pinyin), dtype=bool)
sns.heatmap(agreement_tubule, annot=True, fmt='.3f', cmap='Blues',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0.4, vmax=1.0, cbar_kws={'label': 'Agreement Rate'},
            ax=axes[0], linewidths=0.5, linecolor='white',
            cbar=True, square=True)
axes[0].set_title('Tubule Type - Agreement Rate', 
                  fontsize=12, fontweight='bold', pad=12, family='Arial')
axes[0].set_xlabel('Annotator', fontsize=11, fontweight='bold')
axes[0].set_ylabel('Annotator', fontsize=11, fontweight='bold')

# Kappa系数热图
sns.heatmap(kappa_tubule, annot=True, fmt='.3f', cmap='RdYlGn',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0, vmax=1, cbar_kws={'label': "Cohen's Kappa"},
            ax=axes[1], linewidths=0.5, linecolor='white',
            cbar=True, square=True)
axes[1].set_title("Tubule Type - Cohen's Kappa", 
                  fontsize=12, fontweight='bold', pad=12, family='Arial')
axes[1].set_xlabel('Annotator', fontsize=11, fontweight='bold')
axes[1].set_ylabel('Annotator', fontsize=11, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_Consistency_TubuleType.png', dpi=300, bbox_inches='tight', facecolor='white')
print("\n✓ Saved: Nature_Consistency_TubuleType.png")
plt.close()

# 图2: 萎缩状态一致性热图
fig, axes = plt.subplots(1, 2, figsize=(14, 5.5))
fig.patch.set_facecolor('white')

# 简单一致率热图
sns.heatmap(agreement_atrophy, annot=True, fmt='.3f', cmap='Blues',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0.7, vmax=1.0, cbar_kws={'label': 'Agreement Rate'},
            ax=axes[0], linewidths=0.5, linecolor='white',
            cbar=True, square=True)
axes[0].set_title('Atrophy Status - Agreement Rate', 
                  fontsize=12, fontweight='bold', pad=12, family='Arial')
axes[0].set_xlabel('Annotator', fontsize=11, fontweight='bold')
axes[0].set_ylabel('Annotator', fontsize=11, fontweight='bold')

# Kappa系数热图
sns.heatmap(kappa_atrophy, annot=True, fmt='.3f', cmap='RdYlGn',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0, vmax=1, cbar_kws={'label': "Cohen's Kappa"},
            ax=axes[1], linewidths=0.5, linecolor='white',
            cbar=True, square=True)
axes[1].set_title("Atrophy Status - Cohen's Kappa", 
                  fontsize=12, fontweight='bold', pad=12, family='Arial')
axes[1].set_xlabel('Annotator', fontsize=11, fontweight='bold')
axes[1].set_ylabel('Annotator', fontsize=11, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_Consistency_AtrophyStatus.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_Consistency_AtrophyStatus.png")
plt.close()

# ====================
# 3. 各标注者的类别分布 - Nature风格
# ====================

fig, axes = plt.subplots(2, 2, figsize=(16, 10))
fig.patch.set_facecolor('white')

# 小管类型分布
for idx, (user, user_py) in enumerate(zip(target_users, target_users_pinyin)):
    ax = axes[idx // 2, idx % 2]
    user_data = df_filtered[df_filtered['用户名'] == user]
    tubule_counts = user_data['小管类型'].value_counts()
    
    # 使用Nature配色
    colors_bar = [NATURE_COLORS['primary'][i % len(NATURE_COLORS['primary'])] 
                  for i in range(len(tubule_counts))]
    bars = ax.bar(range(len(tubule_counts)), tubule_counts.values, 
                   color=colors_bar, edgecolor='white', linewidth=1.5, alpha=0.85)
    ax.set_xticks(range(len(tubule_counts)))
    ax.set_xticklabels(tubule_counts.index, rotation=45, ha='right', fontsize=9)
    ax.set_title(f'{user_py} - Tubule Type Distribution (n={len(user_data)})', 
                 fontsize=11, fontweight='bold', family='Arial')
    ax.set_ylabel('Count', fontsize=10, fontweight='bold')
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.grid(axis='y', alpha=0.3, linestyle='--', linewidth=0.8)
    
    # 在柱子上显示数值
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
                f'{int(height)}',
                ha='center', va='bottom', fontsize=8, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_Distribution_TubuleType.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_Distribution_TubuleType.png")
plt.close()

# 萎缩状态分布
fig, axes = plt.subplots(2, 2, figsize=(16, 9))
fig.patch.set_facecolor('white')

for idx, (user, user_py) in enumerate(zip(target_users, target_users_pinyin)):
    ax = axes[idx // 2, idx % 2]
    user_data = df_filtered[df_filtered['用户名'] == user]
    atrophy_counts = user_data['萎缩状态'].value_counts()
    
    # 使用Nature配色
    colors_bar = [NATURE_COLORS['primary'][i % len(NATURE_COLORS['primary'])] 
                  for i in range(len(atrophy_counts))]
    bars = ax.bar(range(len(atrophy_counts)), atrophy_counts.values, 
                   color=colors_bar, edgecolor='white', linewidth=1.5, alpha=0.85)
    ax.set_xticks(range(len(atrophy_counts)))
    ax.set_xticklabels(atrophy_counts.index, rotation=0, ha='center', fontsize=10)
    ax.set_title(f'{user_py} - Atrophy Status Distribution (n={len(user_data)})', 
                 fontsize=11, fontweight='bold', family='Arial')
    ax.set_ylabel('Count', fontsize=10, fontweight='bold')
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.grid(axis='y', alpha=0.3, linestyle='--', linewidth=0.8)
    
    # 在柱子上显示数值和百分比
    for bar in bars:
        height = bar.get_height()
        percentage = height / len(user_data) * 100
        ax.text(bar.get_x() + bar.get_width()/2., height,
                f'{int(height)}\n({percentage:.1f}%)',
                ha='center', va='bottom', fontsize=9, fontweight='bold')

plt.tight_layout()
plt.savefig('Nature_Distribution_AtrophyStatus.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_Distribution_AtrophyStatus.png")
plt.close()

# ====================
# 4. 混淆矩阵 (两两比较) - Nature风格
# ====================

def plot_confusion_matrices_nature(data, annotators, title_prefix, filename):
    """绘制所有两两标注者之间的混淆矩阵 - Nature风格"""
    pairs = list(combinations(annotators, 2))
    n_pairs = len(pairs)
    
    # 计算合适的子图布局
    n_cols = 3
    n_rows = (n_pairs + n_cols - 1) // n_cols
    
    fig, axes = plt.subplots(n_rows, n_cols, figsize=(16, 5.5*n_rows))
    fig.patch.set_facecolor('white')
    axes = axes.flatten() if n_pairs > 1 else [axes]
    
    for idx, (ann1, ann2) in enumerate(pairs):
        ax = axes[idx]
        
        # 获取两个标注者都完成的样本
        valid_samples = data[[ann1, ann2]].dropna()
        
        if len(valid_samples) > 0:
            labels1 = valid_samples[ann1].values
            labels2 = valid_samples[ann2].values
            
            # 获取所有唯一的类别
            all_labels = sorted(set(labels1) | set(labels2))
            
            # 计算混淆矩阵
            cm = confusion_matrix(labels1, labels2, labels=all_labels)
            
            # 归一化为百分比
            cm_normalized = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis] * 100
            
            # 绘制热图
            sns.heatmap(cm_normalized, annot=cm, fmt='d', cmap='Blues',
                       xticklabels=all_labels, yticklabels=all_labels,
                       cbar_kws={'label': 'Percentage (%)'}, ax=ax,
                       linewidths=0.5, linecolor='white',
                       vmin=0, vmax=100)
            
            # 计算一致率
            agreement = np.mean(labels1 == labels2)
            kappa = cohen_kappa_score(labels1, labels2)
            
            ax.set_title(f'{ann1} vs {ann2}\nAgreement: {agreement:.3f}, κ: {kappa:.3f}',
                        fontsize=10, fontweight='bold', family='Arial')
            ax.set_xlabel(f'{ann2} annotation', fontsize=9, fontweight='bold')
            ax.set_ylabel(f'{ann1} annotation', fontsize=9, fontweight='bold')
    
    # 隐藏多余的子图
    for idx in range(n_pairs, len(axes)):
        axes[idx].axis('off')
    
    plt.tight_layout()
    plt.savefig(filename, dpi=300, bbox_inches='tight', facecolor='white')
    print(f"✓ Saved: {filename}")
    plt.close()

# 绘制小管类型的混淆矩阵
plot_confusion_matrices_nature(
    pivot_tubule[target_users_pinyin], 
    target_users_pinyin,
    'Tubule Type',
    'Nature_ConfusionMatrix_TubuleType.png'
)

# 绘制萎缩状态的混淆矩阵
plot_confusion_matrices_nature(
    pivot_atrophy[target_users_pinyin],
    target_users_pinyin,
    'Atrophy Status',
    'Nature_ConfusionMatrix_AtrophyStatus.png'
)

# ====================
# 5. 综合可视化仪表盘 - Nature风格
# ====================

fig = plt.figure(figsize=(18, 11))
fig.patch.set_facecolor('white')
gs = fig.add_gridspec(3, 3, hspace=0.35, wspace=0.35)

# 1. 小管类型一致率热图
ax1 = fig.add_subplot(gs[0, 0])
sns.heatmap(agreement_tubule, annot=True, fmt='.2f', cmap='Blues',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0.4, vmax=1, cbar=False, ax=ax1, square=True,
            linewidths=0.5, linecolor='white')
ax1.set_title('Tubule - Agreement', fontsize=10, fontweight='bold', family='Arial')
ax1.tick_params(labelsize=8)

# 2. 小管类型Kappa热图
ax2 = fig.add_subplot(gs[0, 1])
sns.heatmap(kappa_tubule, annot=True, fmt='.2f', cmap='RdYlGn',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0, vmax=1, cbar=False, ax=ax2, square=True,
            linewidths=0.5, linecolor='white')
ax2.set_title('Tubule - Kappa', fontsize=10, fontweight='bold', family='Arial')
ax2.tick_params(labelsize=8)

# 3. 萎缩状态一致率热图
ax3 = fig.add_subplot(gs[0, 2])
sns.heatmap(agreement_atrophy, annot=True, fmt='.2f', cmap='Blues',
            xticklabels=target_users_pinyin, yticklabels=target_users_pinyin,
            vmin=0.7, vmax=1, cbar=False, ax=ax3, square=True,
            linewidths=0.5, linecolor='white')
ax3.set_title('Atrophy - Agreement', fontsize=10, fontweight='bold', family='Arial')
ax3.tick_params(labelsize=8)

# 4-7. 各标注者小管类型分布
for idx, (user, user_py) in enumerate(zip(target_users, target_users_pinyin)):
    row = 1 + idx // 3
    col = idx % 3
    ax = fig.add_subplot(gs[row, col])
        
    user_data = df_filtered[df_filtered['用户名'] == user]
    tubule_counts = user_data['小管类型'].value_counts().head(8)
    
    colors_bar = [NATURE_COLORS['primary'][i % len(NATURE_COLORS['primary'])] 
                  for i in range(len(tubule_counts))]
    bars = ax.barh(range(len(tubule_counts)), tubule_counts.values, 
                    color=colors_bar, edgecolor='white', linewidth=1, alpha=0.85)
    ax.set_yticks(range(len(tubule_counts)))
    ax.set_yticklabels(tubule_counts.index, fontsize=8)
    ax.set_title(f'{user_py} (n={len(user_data)})', fontsize=9, fontweight='bold', family='Arial')
    ax.set_xlabel('Count', fontsize=8, fontweight='bold')
    ax.invert_yaxis()
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.grid(axis='x', alpha=0.3, linestyle='--', linewidth=0.5)
    
    for i, bar in enumerate(bars):
        width = bar.get_width()
        ax.text(width + 0.5, bar.get_y() + bar.get_height()/2.,
                f'{int(width)}',
                ha='left', va='center', fontsize=7, fontweight='bold')

# 8. 萎缩状态对比
ax8 = fig.add_subplot(gs[2, :])
atrophy_summary = []
for user, user_py in zip(target_users, target_users_pinyin):
    user_data = df_filtered[df_filtered['用户名'] == user]
    atrophy_dist = user_data['萎缩状态'].value_counts()
    for status, count in atrophy_dist.items():
        atrophy_summary.append({
            'Annotator': user_py,
            'Atrophy Status': status,
            'Count': count
        })

df_atrophy_summary = pd.DataFrame(atrophy_summary)
pivot_plot = df_atrophy_summary.pivot(index='Atrophy Status', columns='Annotator', values='Count')

x = np.arange(len(pivot_plot.index))
width = 0.2
for i, annotator in enumerate(target_users_pinyin):
    if annotator in pivot_plot.columns:
        values = pivot_plot[annotator].values
        ax8.bar(x + i*width, values, width, 
               label=annotator, color=NATURE_COLORS['primary'][i],
               edgecolor='white', linewidth=1, alpha=0.85)

ax8.set_xlabel('Atrophy Status', fontsize=10, fontweight='bold')
ax8.set_ylabel('Count', fontsize=10, fontweight='bold')
ax8.set_title('Atrophy Status Comparison', fontsize=11, fontweight='bold', family='Arial')
ax8.set_xticks(x + width * 1.5)
ax8.set_xticklabels(pivot_plot.index, fontsize=9)
ax8.legend(title='Annotator', fontsize=8, title_fontsize=9, 
          loc='upper right', framealpha=0.9)
ax8.spines['top'].set_visible(False)
ax8.spines['right'].set_visible(False)
ax8.grid(axis='y', alpha=0.3, linestyle='--', linewidth=0.8)

# 添加总标题
fig.suptitle('Annotation Consistency Analysis Dashboard', 
            fontsize=14, fontweight='bold', y=0.995, family='Arial')

plt.savefig('Nature_Dashboard.png', dpi=300, bbox_inches='tight', facecolor='white')
print("✓ Saved: Nature_Dashboard.png")
plt.close()

# ====================
# 6. 生成英文报告
# ====================

def create_summary_report_english():
    """创建英文综合统计报告"""
    
    report = []
    report.append("=" * 80)
    report.append("Annotation Consistency Analysis Report")
    report.append("=" * 80)
    report.append("")
    
    # 基本统计
    report.append("I. Basic Statistics")
    report.append("-" * 80)
    for user, user_py in zip(target_users, target_users_pinyin):
        user_data = df_filtered[df_filtered['用户名'] == user]
        report.append(f"{user_py}: Completed {len(user_data)} images")
    report.append("")
    report.append(f"Common images annotated by all 4 annotators:")
    report.append(f"  - Tubule type: {len(common_images_tubule)} images")
    report.append(f"  - Atrophy status: {len(common_images_atrophy)} images")
    report.append("")
    
    # 小管类型一致性
    report.append("II. Tubule Type Annotation Consistency")
    report.append("-" * 80)
    
    # 计算平均一致率和Kappa
    upper_tri_indices = np.triu_indices(len(target_users), k=1)
    avg_agreement = np.mean(agreement_tubule[upper_tri_indices])
    avg_kappa = np.mean(kappa_tubule[upper_tri_indices])
    
    report.append(f"Average agreement rate: {avg_agreement:.3f} ({avg_agreement*100:.1f}%)")
    report.append(f"Average Kappa coefficient: {avg_kappa:.3f}")
    report.append("")
    
    report.append("Kappa coefficient interpretation:")
    report.append("  < 0.00   : No agreement")
    report.append("  0.00-0.20: Slight agreement")
    report.append("  0.21-0.40: Fair agreement")
    report.append("  0.41-0.60: Moderate agreement")
    report.append("  0.61-0.80: Substantial agreement")
    report.append("  0.81-1.00: Almost perfect agreement")
    report.append("")
    
    report.append("Pairwise annotator consistency:")
    for i, ann1 in enumerate(target_users_pinyin):
        for j, ann2 in enumerate(target_users_pinyin):
            if i < j:
                report.append(f"  {ann1} vs {ann2}: "
                            f"Agreement={agreement_tubule[i,j]:.3f}, "
                            f"Kappa={kappa_tubule[i,j]:.3f}")
    report.append("")
    
    # 萎缩状态一致性
    report.append("III. Atrophy Status Annotation Consistency")
    report.append("-" * 80)
    
    avg_agreement_atr = np.mean(agreement_atrophy[upper_tri_indices])
    avg_kappa_atr = np.mean(kappa_atrophy[upper_tri_indices])
    
    report.append(f"Average agreement rate: {avg_agreement_atr:.3f} ({avg_agreement_atr*100:.1f}%)")
    report.append(f"Average Kappa coefficient: {avg_kappa_atr:.3f}")
    report.append("")
    
    report.append("Pairwise annotator consistency:")
    for i, ann1 in enumerate(target_users_pinyin):
        for j, ann2 in enumerate(target_users_pinyin):
            if i < j:
                report.append(f"  {ann1} vs {ann2}: "
                            f"Agreement={agreement_atrophy[i,j]:.3f}, "
                            f"Kappa={kappa_atrophy[i,j]:.3f}")
    report.append("")
    
    # 标注类别统计
    report.append("IV. Annotation Category Distribution")
    report.append("-" * 80)
    report.append("Tubule type distribution:")
    for user, user_py in zip(target_users, target_users_pinyin):
        user_data = df_filtered[df_filtered['用户名'] == user]
        report.append(f"\n  {user_py}:")
        tubule_dist = user_data['小管类型'].value_counts()
        for cat, count in tubule_dist.items():
            pct = count / len(user_data) * 100
            report.append(f"    {cat}: {count} ({pct:.1f}%)")
    
    report.append("\nAtrophy status distribution:")
    for user, user_py in zip(target_users, target_users_pinyin):
        user_data = df_filtered[df_filtered['用户名'] == user]
        report.append(f"\n  {user_py}:")
        atrophy_dist = user_data['萎缩状态'].value_counts()
        for cat, count in atrophy_dist.items():
            pct = count / len(user_data) * 100
            report.append(f"    {cat}: {count} ({pct:.1f}%)")
    
    report.append("")
    report.append("=" * 80)
    report.append("Report Generation Completed")
    report.append("=" * 80)
    
    return "\n".join(report)

# 生成并保存报告
report_text = create_summary_report_english()
print("\n" + report_text)

with open('Nature_ConsistencyReport.txt', 'w', encoding='utf-8') as f:
    f.write(report_text)
print("\n✓ Saved: Nature_ConsistencyReport.txt")

print("\n" + "="*80)
print("All analyses completed! Generated files (Nature style):")
print("  1. Nature_Consistency_TubuleType.png")
print("  2. Nature_Consistency_AtrophyStatus.png")
print("  3. Nature_Distribution_TubuleType.png")
print("  4. Nature_Distribution_AtrophyStatus.png")
print("  5. Nature_ConfusionMatrix_TubuleType.png")
print("  6. Nature_ConfusionMatrix_AtrophyStatus.png")
print("  7. Nature_Dashboard.png")
print("  8. Nature_ConsistencyReport.txt")
print("="*80)

