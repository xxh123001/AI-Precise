package com.example.tag_backend.enums;

/**
 * 用户状态枚举
 * 定义用户账号的活跃状态
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public enum UserStatus {
    /**
     * 活跃状态
     */
    ACTIVE("活跃"),
    
    /**
     * 不活跃状态
     */
    INACTIVE("禁用");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
