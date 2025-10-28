package com.example.tag_backend.enums;

/**
 * 图像分配状态枚举
 * 定义图像标注任务的状态
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public enum AssignmentStatus {
    /**
     * 已分配
     */
    ASSIGNED("已分配"),
    
    /**
     * 进行中
     */
    IN_PROGRESS("进行中"),
    
    /**
     * 已完成
     */
    COMPLETED("已完成");

    private final String description;

    AssignmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
