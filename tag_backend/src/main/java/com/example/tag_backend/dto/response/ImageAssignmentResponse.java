package com.example.tag_backend.dto.response;

import com.example.tag_backend.enums.AssignmentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图像分配任务响应DTO
 * 用于返回用户的标注任务信息
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssignmentResponse {

    /**
     * 分配记录ID
     */
    private Long id;

    /**
     * 图像信息
     */
    private ImageResponse image;

    /**
     * 分配状态
     */
    private AssignmentStatus status;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 分配者用户名
     */
    private String assignerUsername;

    /**
     * 是否有标注记录
     */
    private Boolean hasAnnotation;

    /**
     * 标注完成进度百分比
     */
    private Integer progress;
}
