package com.example.tag_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 仪表板统计响应DTO
 * 根据需求文档5.1节管理员界面仪表盘要求设计
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    /**
     * 用户统计信息
     */
    private UserStats userStats;

    /**
     * 图像统计信息
     */
    private ImageStats imageStats;

    /**
     * 标注统计信息
     */
    private AnnotationStats annotationStats;

    /**
     * 系统状态信息
     */
    private SystemStats systemStats;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        /**
         * 总用户数
         */
        private Long totalUsers;

        /**
         * 活跃用户数
         */
        private Long activeUsers;

        /**
         * 当前在线用户数
         */
        private Long onlineUsers;

        /**
         * 新注册用户（本周）
         */
        private Long newUsersThisWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageStats {
        /**
         * 总图像数
         */
        private Long totalImages;

        /**
         * 已分配图像数
         */
        private Long assignedImages;

        /**
         * 已完成标注图像数
         */
        private Long completedImages;

        /**
         * 待标注图像数
         */
        private Long pendingImages;

        /**
         * 本周上传图像数
         */
        private Long imagesUploadedThisWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnotationStats {
        /**
         * 总标注数
         */
        private Long totalAnnotations;

        /**
         * 已完成标注数
         */
        private Long completedAnnotations;

        /**
         * 草稿标注数
         */
        private Long draftAnnotations;

        /**
         * 标注完成率
         */
        private Double completionRate;

        /**
         * 本周完成标注数
         */
        private Long annotationsCompletedThisWeek;

        /**
         * 平均标注时间（分钟）
         */
        private Double averageAnnotationTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStats {
        /**
         * 系统运行状态
         */
        private String systemStatus;

        /**
         * 数据库连接状态
         */
        private String databaseStatus;

        /**
         * Redis连接状态
         */
        private String redisStatus;

        /**
         * 活跃标签配置数
         */
        private Long activeLabelConfigs;

        /**
         * 系统启动时间
         */
        private LocalDateTime systemStartTime;

        /**
         * 存储使用情况
         */
        private Map<String, Object> storageInfo;
    }
}
