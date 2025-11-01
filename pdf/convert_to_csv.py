#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将Excel文件转换为CSV格式，只保留指定列
"""

import pandas as pd
import os

def convert_excel_to_csv(input_file, output_file, columns_to_keep):
    """
    将Excel文件转换为CSV格式，只保留指定的列
    
    Args:
        input_file: 输入的Excel文件路径
        output_file: 输出的CSV文件路径
        columns_to_keep: 要保留的列名列表
    """
    try:
        # 读取Excel文件
        print(f"正在读取 {input_file}...")
        df = pd.read_excel(input_file)
        
        # 只保留指定的列
        df_filtered = df[columns_to_keep]
        
        # 保存为CSV格式
        df_filtered.to_csv(output_file, index=False, encoding='utf-8-sig')
        print(f"✓ 成功转换: {output_file}")
        print(f"  共 {len(df_filtered)} 行数据")
        
    except Exception as e:
        print(f"✗ 转换失败 {input_file}: {str(e)}")

if __name__ == "__main__":
    # 要保留的列
    columns = ['病理号', '病理诊断', 'EM诊断']
    
    # 转换2023年的文件
    convert_excel_to_csv(
        '2023年 .xlsx',
        '2023年.csv',
        columns
    )
    
    # 转换2024年的文件
    convert_excel_to_csv(
        '2024.xlsx',
        '2024年.csv',
        columns
    )
    
    print("\n转换完成！")

