package com.example.tag_backend.service;

import com.example.tag_backend.dto.request.LoginRequest;
import com.example.tag_backend.dto.response.LoginResponse;
import com.example.tag_backend.dto.response.UserResponse;

/**
 * 认证服务接口
 * 定义用户认证相关的业务操作
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface AuthService {

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应，包含令牌和用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     * 
     * @param username 用户名
     * @return 当前用户信息
     */
    UserResponse getCurrentUser(String username);

    /**
     * 刷新令牌
     * 
     * @param username 用户名
     * @return 新的登录响应
     */
    LoginResponse refreshToken(String username);

    /**
     * 用户登出
     * 
     * @param token JWT令牌
     */
    void logout(String token);
}
