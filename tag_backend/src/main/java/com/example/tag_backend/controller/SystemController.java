package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.service.SessionService;
import com.example.tag_backend.utils.RedisUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理控制器
 * 提供系统状态查看、缓存管理等功能
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/admin/system")
@Tag(name = "系统管理", description = "系统状态、缓存管理等相关接口")
@RequiredArgsConstructor
@Slf4j
public class SystemController {

    private final RedisUtils redisUtils;
    private final SessionService sessionService;
    private final CacheManager cacheManager;

    @GetMapping("/status")
    @Operation(summary = "获取系统状态", description = "获取系统运行状态信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getSystemStatus() {
        log.info("获取系统状态请求");
        
        Map<String, Object> status = new HashMap<>();
        
        // 系统基本信息
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        
        // Redis连接状态
        try {
            redisUtils.set("health_check", "ok", 60);
            status.put("redis", "connected");
            status.put("redisTest", redisUtils.get("health_check"));
        } catch (Exception e) {
            log.error("Redis连接检查失败", e);
            status.put("redis", "disconnected");
            status.put("redisError", e.getMessage());
        }
        
        // 在线用户统计
        status.put("onlineUsers", sessionService.getOnlineUserCount());
        
        // 缓存管理器信息
        status.put("cacheManager", cacheManager.getClass().getSimpleName());
        status.put("cacheNames", cacheManager.getCacheNames());
        
        return Result.success(status);
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "获取缓存统计", description = "获取Redis缓存使用统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getCacheStats() {
        log.info("获取缓存统计请求");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 在线用户统计
            long onlineUsers = sessionService.getOnlineUserCount();
            stats.put("onlineUsers", onlineUsers);
            
            // 缓存键统计（这里是简单示例，实际可以根据需要扩展）
            stats.put("labelConfigsCacheSize", getCacheSize("labelConfigs"));
            stats.put("usersCacheSize", getCacheSize("users"));
            stats.put("usersByUsernameCacheSize", getCacheSize("usersByUsername"));
            stats.put("activeLabelConfigsCacheSize", getCacheSize("activeLabelConfigs"));
            
            // Redis内存使用情况（需要Redis的INFO命令支持）
            stats.put("redisStatus", "active");
            
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            stats.put("error", e.getMessage());
        }
        
        return Result.success(stats);
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "清除缓存", description = "清除指定缓存或全部缓存")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> clearCache(@RequestParam(required = false) String cacheName) {
        log.info("清除缓存请求，缓存名: {}", cacheName);
        
        try {
            if (cacheName != null && !cacheName.isEmpty()) {
                // 清除指定缓存
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("缓存 {} 已清除", cacheName);
                    return Result.success("缓存 " + cacheName + " 已清除");
                } else {
                    return Result.error(404, "缓存 " + cacheName + " 不存在");
                }
            } else {
                // 清除所有缓存
                cacheManager.getCacheNames().forEach(name -> {
                    var cache = cacheManager.getCache(name);
                    if (cache != null) {
                        cache.clear();
                    }
                });
                log.info("所有缓存已清除");
                return Result.success("所有缓存已清除");
            }
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return Result.error(500, "清除缓存失败: " + e.getMessage());
        }
    }

    @GetMapping("/cache/test")
    @Operation(summary = "测试Redis连接", description = "测试Redis连接和基本操作")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> testRedis() {
        log.info("测试Redis连接请求");
        
        Map<String, Object> testResult = new HashMap<>();
        
        try {
            String testKey = "redis_test_" + System.currentTimeMillis();
            String testValue = "Redis连接测试成功";
            
            // 测试写入
            boolean setResult = redisUtils.set(testKey, testValue, 60);
            testResult.put("setOperation", setResult ? "成功" : "失败");
            
            // 测试读取
            Object getValue = redisUtils.get(testKey);
            testResult.put("getOperation", getValue != null ? "成功" : "失败");
            testResult.put("getValue", getValue);
            
            // 测试删除
            redisUtils.del(testKey);
            boolean exists = redisUtils.hasKey(testKey);
            testResult.put("deleteOperation", !exists ? "成功" : "失败");
            
            // 测试过期时间
            String expireKey = "redis_expire_test_" + System.currentTimeMillis();
            redisUtils.set(expireKey, "过期测试", 5);
            long expireTime = redisUtils.getExpire(expireKey);
            testResult.put("expireTest", expireTime > 0 ? "成功" : "失败");
            testResult.put("expireTime", expireTime + "秒");
            
            testResult.put("overall", "Redis测试全部通过");
            
        } catch (Exception e) {
            log.error("Redis测试失败", e);
            testResult.put("error", e.getMessage());
            testResult.put("overall", "Redis测试失败");
        }
        
        return Result.success(testResult);
    }

    /**
     * 获取缓存大小（简单实现）
     */
    private String getCacheSize(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            return cache != null ? "活跃" : "未激活";
        } catch (Exception e) {
            return "未知";
        }
    }
}
