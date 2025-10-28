package com.example.tag_backend.enums;

/**
 * 用户角色枚举
 * 定义系统中用户的不同角色
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public enum UserRole {
    /**
     * 管理员角色
     */
    ADMIN("管理员"),
    
    /**
     * 普通用户角色
     */
    USER("普通用户");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
