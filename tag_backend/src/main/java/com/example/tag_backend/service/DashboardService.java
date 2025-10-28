package com.example.tag_backend.service;

import com.example.tag_backend.dto.response.ActivityResponse;
import com.example.tag_backend.dto.response.DashboardStatsResponse;

import java.util.List;

/**
 * 仪表板服务接口
 * 根据需求文档5.1节管理员界面仪表盘功能要求设计
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface DashboardService {

    /**
     * 获取仪表板统计数据
     * 
     * @return 仪表板统计信息
     */
    DashboardStatsResponse getDashboardStats();

    /**
     * 获取实时系统状态
     * 
     * @return 系统状态信息
     */
    DashboardStatsResponse.SystemStats getSystemStatus();

    /**
     * 获取标注进度统计
     * 
     * @return 标注进度统计
     */
    DashboardStatsResponse.AnnotationStats getAnnotationProgress();

    /**
     * 获取最近活动记录
     * 
     * @param limit 限制数量
     * @return 最近活动列表
     */
    List<ActivityResponse> getRecentActivities(Integer limit);
}
