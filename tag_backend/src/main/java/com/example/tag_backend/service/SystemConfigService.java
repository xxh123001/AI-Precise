package com.example.tag_backend.service;

import java.util.Map;

/**
 * 系统配置服务接口
 * 
 * @author Felix
 * @version 1.0
 * @since 2025-10-21
 */
public interface SystemConfigService {
    
    /**
     * 获取配置值
     */
    String getConfigValue(String key);
    
    /**
     * 获取配置值（带默认值）
     */
    String getConfigValue(String key, String defaultValue);
    
    /**
     * 设置配置值
     */
    void setConfigValue(String key, String value);
    
    /**
     * 获取全局裁剪配置
     */
    Map<String, Object> getGlobalCropConfig();
    
    /**
     * 更新全局裁剪配置
     */
    void updateGlobalCropConfig(Integer cropX, Integer cropY, Integer cropWidth, Integer cropHeight, Boolean enabled);
}

