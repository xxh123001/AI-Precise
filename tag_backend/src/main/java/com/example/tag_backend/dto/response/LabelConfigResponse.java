package com.example.tag_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签配置响应DTO
 * 用于返回标签配置信息
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelConfigResponse {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 标签配置JSON数据
     */
    private String config;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建者用户名
     */
    private String creatorUsername;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 使用此配置的图像数量
     */
    private Long imageCount = 0L;
}
