package com.example.tag_backend.service;

import com.example.tag_backend.dto.response.UserResponse;

/**
 * 会话管理服务接口
 * 负责用户会话的缓存管理
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface SessionService {

    /**
     * 缓存用户会话信息
     * 
     * @param token 令牌
     * @param userResponse 用户信息
     * @param expireSeconds 过期时间（秒）
     */
    void cacheUserSession(String token, UserResponse userResponse, long expireSeconds);

    /**
     * 获取用户会话信息
     * 
     * @param token 令牌
     * @return 用户信息
     */
    UserResponse getUserSession(String token);

    /**
     * 移除用户会话信息
     * 
     * @param token 令牌
     */
    void removeUserSession(String token);

    /**
     * 刷新用户会话过期时间
     * 
     * @param token 令牌
     * @param expireSeconds 过期时间（秒）
     */
    void refreshUserSession(String token, long expireSeconds);

    /**
     * 检查用户会话是否存在
     * 
     * @param token 令牌
     * @return 是否存在
     */
    boolean isSessionExists(String token);

    /**
     * 缓存用户在线状态
     * 
     * @param userId 用户ID
     * @param isOnline 是否在线
     */
    void cacheUserOnlineStatus(Long userId, boolean isOnline);

    /**
     * 获取用户在线状态
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean getUserOnlineStatus(Long userId);

    /**
     * 获取所有在线用户数量
     * 
     * @return 在线用户数量
     */
    long getOnlineUserCount();
}
