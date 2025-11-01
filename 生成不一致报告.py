#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成任务4的标注不一致报告CSV
分析5位老师（任雅丽、喻小娟、张旭、王惠、蒋镭）的标注一致性
"""

import mysql.connector
import pandas as pd
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
    """标准化标注数据，提取关键信息"""
    if not ann_data:
        return None
    
    # 提取小管类型
    tubule_type = ann_data.get('小管1', '') or ann_data.get('小管', '')
    atrophy = ann_data.get('萎缩', '')
    
    # 创建标准化的标注字符串
    parts = []
    if tubule_type:
        parts.append(f"小管:{tubule_type}")
    if atrophy:
        parts.append(f"萎缩:{atrophy}")
    
    return '|'.join(parts) if parts else None

def annotation_to_dict(ann_data):
    """将标注数据转换为字典格式用于显示"""
    if not ann_data:
        return {}
    
    result = {}
    
    # 提取小管类型
    tubule_type = ann_data.get('小管1', '') or ann_data.get('小管', '')
    if tubule_type:
        result['小管类型'] = tubule_type
    
    # 提取萎缩
    atrophy = ann_data.get('萎缩', '')
    if atrophy:
        result['萎缩'] = atrophy
    
    return result

def generate_inconsistency_report():
    """生成不一致报告"""
    
    print("="*80)
    print("📊 生成任务4标注不一致报告")
    print("="*80)
    print()
    
    conn = get_db_connection()
    if not conn:
        return
    
    try:
        # 定义5位老师
        teachers = ['任雅丽', '喻小娟', '张旭', '王惠', '蒋镭']
        
        print(f"📋 分析对象：{', '.join(teachers)}")
        print()
        
        # 获取标注数据
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
        
        print("📊 正在获取标注数据...")
        df_annotations = pd.read_sql(query, conn)
        print(f"✅ 获取到 {len(df_annotations)} 条标注记录")
        print()
        
        # 按图片分组分析
        annotations_by_image = {}
        
        for _, row in df_annotations.iterrows():
            image_id = row['image_id']
            username = row['username']
            ann_data = parse_annotation_data(row['annotation_data'])
            
            if image_id not in annotations_by_image:
                annotations_by_image[image_id] = {
                    'filename': row['filename'],
                    'annotations': {}
                }
            
            if ann_data:
                # 标准化标注用于分组
                normalized = normalize_annotation(ann_data)
                # 原始数据用于显示
                annotations_by_image[image_id]['annotations'][username] = {
                    'normalized': normalized,
                    'original': ann_data
                }
        
        # 分析不一致的图片
        inconsistent_results = []
        consistent_count = 0
        
        for image_id, data in sorted(annotations_by_image.items()):
            filename = data['filename']
            anns = data['annotations']
            
            # 只分析5人都标注的图片
            if len(anns) != 5:
                continue
            
            # 按标注内容分组
            groups = defaultdict(list)
            for username, ann_info in anns.items():
                normalized = ann_info['normalized']
                groups[normalized].append(username)
            
            # 如果只有一组，说明完全一致
            if len(groups) == 1:
                consistent_count += 1
                continue
            
            # 存在不一致
            total_annotators = len(anns)
            different_types = len(groups)
            
            # 生成分组详情
            group_details = []
            group_contents = []
            
            for normalized, usernames in sorted(groups.items(), key=lambda x: -len(x[1])):
                # 分组详情
                user_count = len(usernames)
                user_list = ', '.join(usernames)
                group_details.append(f"【{user_count}人】{user_list}")
                
                # 分组标注内容
                first_user = usernames[0]
                ann_dict = annotation_to_dict(anns[first_user]['original'])
                
                # 格式化标注内容
                content_parts = []
                if '小管类型' in ann_dict:
                    content_parts.append(f'"小管类型": "{ann_dict["小管类型"]}"')
                if '萎缩' in ann_dict:
                    content_parts.append(f'"萎缩": "{ann_dict["萎缩"]}"')
                
                content_str = '{' + ', '.join(content_parts) + '}'
                group_contents.append(f"【{user_list}】: {content_str}")
            
            inconsistent_results.append({
                '图像ID': image_id,
                '文件名': filename,
                '不同标注类型数': different_types,
                '总标注人数': total_annotators,
                '标注分组详情': ' | '.join(group_details),
                '各组标注内容': ' | '.join(group_contents)
            })
        
        # 创建DataFrame
        df_inconsistent = pd.DataFrame(inconsistent_results)
        
        # 添加序号
        df_inconsistent.insert(0, '序号', range(1, len(df_inconsistent) + 1))
        
        # 统计信息
        print("="*80)
        print("📈 统计结果")
        print("="*80)
        print()
        
        total_images = len(annotations_by_image)
        inconsistent_count = len(df_inconsistent)
        
        print(f"📊 总图片数: {total_images} 张")
        print(f"✅ 完全一致: {consistent_count} 张 ({consistent_count/total_images*100:.1f}%)")
        print(f"⚠️  存在不一致: {inconsistent_count} 张 ({inconsistent_count/total_images*100:.1f}%)")
        print()
        
        # 按不同标注类型数分组统计
        if not df_inconsistent.empty:
            print("按不同标注类型数分布：")
            type_dist = df_inconsistent['不同标注类型数'].value_counts().sort_index()
            for types, count in type_dist.items():
                print(f"  {types} 种不同标注: {count} 张图片")
            print()
        
        # 保存结果
        print("="*80)
        print("💾 保存结果")
        print("="*80)
        print()
        
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        
        # 保存不一致报告
        inconsistent_file = f'任务4_标注不一致报告_{timestamp}.csv'
        df_inconsistent.to_csv(inconsistent_file, index=False, encoding='utf-8-sig')
        print(f"✅ 不一致报告已保存: {inconsistent_file}")
        print(f"   包含 {len(df_inconsistent)} 张存在不一致的图片")
        print()
        
        # 生成统计摘要
        summary_file = f'任务4_不一致统计摘要_{timestamp}.txt'
        with open(summary_file, 'w', encoding='utf-8') as f:
            f.write("="*80 + "\n")
            f.write("任务4标注不一致统计摘要\n")
            f.write("="*80 + "\n\n")
            f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"分析对象: {', '.join(teachers)}\n\n")
            
            f.write("一、总体统计\n")
            f.write("-"*80 + "\n")
            f.write(f"总图片数: {total_images} 张\n")
            f.write(f"完全一致: {consistent_count} 张 ({consistent_count/total_images*100:.1f}%)\n")
            f.write(f"存在不一致: {inconsistent_count} 张 ({inconsistent_count/total_images*100:.1f}%)\n\n")
            
            if not df_inconsistent.empty:
                f.write("二、不一致分布\n")
                f.write("-"*80 + "\n")
                for types, count in type_dist.items():
                    f.write(f"{types} 种不同标注: {count} 张图片\n")
                f.write("\n")
            
            f.write("三、文件说明\n")
            f.write("-"*80 + "\n")
            f.write(f"详细报告: {inconsistent_file}\n")
            f.write("格式说明:\n")
            f.write("  - 序号: 不一致图片的编号\n")
            f.write("  - 图像ID: 数据库中的图片ID\n")
            f.write("  - 文件名: 图片文件名\n")
            f.write("  - 不同标注类型数: 有多少种不同的标注结果\n")
            f.write("  - 总标注人数: 标注该图片的总人数\n")
            f.write("  - 标注分组详情: 相同标注的人员分组\n")
            f.write("  - 各组标注内容: 每组的具体标注内容\n")
        
        print(f"✅ 统计摘要已保存: {summary_file}")
        print()
        
        # 显示前10条不一致记录
        if not df_inconsistent.empty:
            print("="*80)
            print("📋 前10条不一致记录（示例）")
            print("="*80)
            print()
            
            display_cols = ['序号', '图像ID', '文件名', '不同标注类型数', '总标注人数']
            print(df_inconsistent[display_cols].head(10).to_string(index=False))
            print()
        
        print("="*80)
        print("✅ 报告生成完成！")
        print("="*80)
        
    finally:
        conn.close()

if __name__ == '__main__':
    generate_inconsistency_report()


