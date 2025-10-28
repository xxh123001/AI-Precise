package com.example.tag_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图像响应DTO
 * 用于返回图像信息
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {

    /**
     * 图像ID
     */
    private Long id;

    /**
     * 文件名（系统生成）
     */
    private String filename;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 上传者ID
     */
    private Long uploadBy;

    /**
     * 上传者用户名
     */
    private String uploaderUsername;

    /**
     * 标签配置ID
     */
    private Long labelConfigId;

    /**
     * 标签配置名称
     */
    private String labelConfigName;

    /**
     * 完整的标签配置信息（在需要时提供）
     */
    private LabelConfigResponse labelConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否已分配
     */
    private Boolean isAssigned = false;

    /**
     * 分配用户列表（用户名）
     */
    private java.util.List<String> assignedUsers = new java.util.ArrayList<>();

    /**
     * 分配用户ID列表
     */
    private java.util.List<Long> assignedUserIds = new java.util.ArrayList<>();

    /**
     * 是否已标注
     */
    private Boolean isAnnotated = false;

    // ========== CSV相关字段 ==========
    
    /**
     * 分类文件夹
     */
    private String folder;

    /**
     * 重新命名的ID
     */
    private String reid;

    /**
     * 图像分类标签
     */
    private String imageClass;

    /**
     * 第一次训练标签
     */
    private String firstTrainLabel;

    /**
     * 测试标签
     */
    private String testLabel;

    /**
     * 模型1
     */
    private String model1;

    /**
     * PAS置信度
     */
    private Double pasConf;

    /**
     * IF标签
     */
    private String ifLabel;

    /**
     * 预测标签
     */
    private String predictLabel;

    /**
     * 预测置信度
     */
    private Double predictConfidence;
}
