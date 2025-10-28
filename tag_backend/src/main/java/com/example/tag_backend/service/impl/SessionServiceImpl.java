package com.example.tag_backend.service.impl;

import com.example.tag_backend.dto.response.UserResponse;
import com.example.tag_backend.service.SessionService;
import com.example.tag_backend.utils.RedisUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 会话管理服务实现类
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";
    private static final String ONLINE_USER_PREFIX = "online:user:";
    private static final String ONLINE_USERS_SET = "online:users";

    @Override
    public void cacheUserSession(String token, UserResponse userResponse, long expireSeconds) {
        try {
            String key = SESSION_PREFIX + token;
            String userJson = objectMapper.writeValueAsString(userResponse);
            redisUtils.set(key, userJson, expireSeconds);
            
            // 同时缓存用户在线状态
            cacheUserOnlineStatus(userResponse.getId(), true);
            
            log.debug("用户会话缓存成功，用户ID: {}, token: {}", userResponse.getId(), token);
        } catch (JsonProcessingException e) {
            log.error("序列化用户信息失败", e);
        }
    }

    @Override
    public UserResponse getUserSession(String token) {
        try {
            String key = SESSION_PREFIX + token;
            String userJson = (String) redisUtils.get(key);
            
            if (userJson != null) {
                UserResponse userResponse = objectMapper.readValue(userJson, UserResponse.class);
                log.debug("获取用户会话成功，用户ID: {}", userResponse.getId());
                return userResponse;
            }
            
            log.debug("用户会话不存在，token: {}", token);
            return null;
        } catch (JsonProcessingException e) {
            log.error("反序列化用户信息失败", e);
            return null;
        }
    }

    @Override
    public void removeUserSession(String token) {
        try {
            // 先获取用户信息，用于清除在线状态
            UserResponse userResponse = getUserSession(token);
            
            String key = SESSION_PREFIX + token;
            redisUtils.del(key);
            
            // 清除用户在线状态
            if (userResponse != null) {
                cacheUserOnlineStatus(userResponse.getId(), false);
            }
            
            log.debug("用户会话已移除，token: {}", token);
        } catch (Exception e) {
            log.error("移除用户会话失败，token: {}", token, e);
        }
    }

    @Override
    public void refreshUserSession(String token, long expireSeconds) {
        String key = SESSION_PREFIX + token;
        redisUtils.expire(key, expireSeconds);
        log.debug("用户会话已刷新，token: {}", token);
    }

    @Override
    public boolean isSessionExists(String token) {
        String key = SESSION_PREFIX + token;
        return redisUtils.hasKey(key);
    }

    @Override
    public void cacheUserOnlineStatus(Long userId, boolean isOnline) {
        String key = ONLINE_USER_PREFIX + userId;
        
        if (isOnline) {
            // 设置用户在线，过期时间设置为30分钟
            redisUtils.set(key, "online", 1800);
            // 添加到在线用户集合
            redisUtils.sSet(ONLINE_USERS_SET, userId.toString());
            redisUtils.expire(ONLINE_USERS_SET, 1800);
        } else {
            // 清除在线状态
            redisUtils.del(key);
            // 从在线用户集合中移除
            redisUtils.setRemove(ONLINE_USERS_SET, userId.toString());
        }
        
        log.debug("用户在线状态已更新，用户ID: {}, 状态: {}", userId, isOnline ? "在线" : "离线");
    }

    @Override
    public boolean getUserOnlineStatus(Long userId) {
        String key = ONLINE_USER_PREFIX + userId;
        return redisUtils.hasKey(key);
    }

    @Override
    public long getOnlineUserCount() {
        return redisUtils.sGetSetSize(ONLINE_USERS_SET);
    }
}
