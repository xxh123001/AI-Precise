package com.example.tag_backend.service.impl;

import com.example.tag_backend.entity.SystemConfig;
import com.example.tag_backend.repository.SystemConfigRepository;
import com.example.tag_backend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置服务实现类
 * 
 * @author Felix
 * @version 1.0
 * @since 2025-10-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public String getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }

    @Override
    @Transactional
    public void setConfigValue(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(new SystemConfig());
        
        config.setConfigKey(key);
        config.setConfigValue(value);
        
        systemConfigRepository.save(config);
        log.info("系统配置已更新: {} = {}", key, value);
    }

    @Override
    public Map<String, Object> getGlobalCropConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("enabled", Boolean.parseBoolean(getConfigValue("global_crop_enabled", "false")));
        config.put("cropX", Integer.parseInt(getConfigValue("global_crop_x", "0")));
        config.put("cropY", Integer.parseInt(getConfigValue("global_crop_y", "0")));
        config.put("cropWidth", Integer.parseInt(getConfigValue("global_crop_width", "909")));
        config.put("cropHeight", Integer.parseInt(getConfigValue("global_crop_height", "1080")));
        config.put("userIds", getConfigValue("global_crop_user_ids", "[]"));
        
        return config;
    }

    @Override
    @Transactional
    public void updateGlobalCropConfig(Integer cropX, Integer cropY, Integer cropWidth, Integer cropHeight, Boolean enabled) {
        log.info("更新全局裁剪配置: enabled={}, x={}, y={}, width={}, height={}", 
                 enabled, cropX, cropY, cropWidth, cropHeight);
        
        setConfigValue("global_crop_enabled", String.valueOf(enabled != null ? enabled : false));
        
        if (cropX != null) {
            setConfigValue("global_crop_x", String.valueOf(cropX));
        }
        if (cropY != null) {
            setConfigValue("global_crop_y", String.valueOf(cropY));
        }
        if (cropWidth != null) {
            setConfigValue("global_crop_width", String.valueOf(cropWidth));
        }
        if (cropHeight != null) {
            setConfigValue("global_crop_height", String.valueOf(cropHeight));
        }
        
        log.info("全局裁剪配置更新成功");
    }
}

