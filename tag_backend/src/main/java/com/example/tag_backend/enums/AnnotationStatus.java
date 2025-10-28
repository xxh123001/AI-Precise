package com.example.tag_backend.enums;

import lombok.Getter;

/**
 * 标注状态枚举
 * 定义标注记录的状态
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Getter
public enum AnnotationStatus {
    /**
     * 草稿状态
     */
    DRAFT("草稿"),
    
    /**
     * 已完成
     */
    COMPLETED("已完成");

    private final String description;

    AnnotationStatus(String description) {
        this.description = description;
    }

}
