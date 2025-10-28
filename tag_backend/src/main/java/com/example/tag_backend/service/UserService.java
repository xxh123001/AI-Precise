package com.example.tag_backend.service;

import com.example.tag_backend.dto.request.CreateUserRequest;
import com.example.tag_backend.dto.request.UpdateUserRequest;
import com.example.tag_backend.dto.request.UserQueryRequest;
import com.example.tag_backend.dto.response.UserResponse;
import com.example.tag_backend.common.result.PageResponse;

/**
 * 用户服务接口
 * 定义用户相关的业务操作
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface UserService {

    /**
     * 创建用户
     * 
     * @param request 用户创建请求
     * @return 创建的用户信息
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * 根据ID获取用户信息
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    UserResponse getUserById(Long id);

    /**
     * 根据用户名获取用户信息
     * 
     * @param username 用户名
     * @return 用户信息
     */
    UserResponse getUserByUsername(String username);

    /**
     * 根据用户名获取用户实体（内部使用）
     * 
     * @param username 用户名
     * @return 用户实体
     */
    com.example.tag_backend.entity.User getUserEntityByUsername(String username);

    /**
     * 根据ID获取用户实体（内部使用）
     * 
     * @param id 用户ID
     * @return 用户实体
     */
    com.example.tag_backend.entity.User getUserEntityById(Long id);

    /**
     * 分页查询用户列表
     * 
     * @param request 查询参数
     * @return 分页用户列表
     */
    PageResponse<UserResponse> getUserList(UserQueryRequest request);

    /**
     * 更新用户信息
     * 
     * @param id 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserResponse updateUser(Long id, CreateUserRequest request);

    /**
     * 更新用户信息（推荐方法）
     * 
     * @param id 用户ID
     * @param request 更新用户请求
     * @return 更新后的用户信息
     */
    UserResponse updateUser(Long id, UpdateUserRequest request);

    /**
     * 删除用户
     * 
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 启用用户
     * 
     * @param id 用户ID
     */
    void enableUser(Long id);

    /**
     * 禁用用户
     * 
     * @param id 用户ID
     */
    void disableUser(Long id);

    /**
     * 重置用户密码
     * 
     * @param id 用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 验证用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    UserResponse validateLogin(String username, String password);
}
