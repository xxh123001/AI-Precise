package com.example.tag_backend.dto.request;

import com.example.tag_backend.enums.UserStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户查询请求DTO
 * 用于接收用户列表查询的参数
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQueryRequest {

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 用户状态
     */
    private UserStatus status;

    /**
     * 页码，从1开始
     */
    private Integer page = 1;

    /**
     * 每页大小，默认10
     */
    private Integer size = 10;

    /**
     * 排序字段，默认按创建时间降序
     */
    private String sort = "createdAt,desc";
}
