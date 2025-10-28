package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.service.SystemConfigService;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 * 
 * @author Felix
 * @version 1.0
 * @since 2025-10-21
 */
@RestController
@RequestMapping("/api")
@Tag(name = "系统配置管理", description = "系统配置相关接口")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/admin/system-config/global-crop")
    @Operation(summary = "获取全局裁剪配置", description = "获取当前的全局裁剪配置（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getGlobalCropConfig() {
        log.info("获取全局裁剪配置请求");
        Map<String, Object> config = systemConfigService.getGlobalCropConfig();
        return Result.success(config);
    }

    @GetMapping("/user/system-config/global-crop")
    @Operation(summary = "获取用户全局裁剪配置", description = "获取当前用户的全局裁剪配置")
    public Result<Map<String, Object>> getUserGlobalCropConfig() {
        try {
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            
            if (user == null) {
                return Result.error(401, "用户未登录");
            }
            
            log.info("用户 {} 获取全局裁剪配置", username);
            
            Map<String, Object> config = systemConfigService.getGlobalCropConfig();
            
            // 检查该用户是否在生效范围内
            String userIdsStr = (String) config.get("userIds");
            boolean appliedToUser = false;
            
            try {
                List<Long> userIds = objectMapper.readValue(userIdsStr, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
                
                // 空数组表示对所有用户生效
                if (userIds.isEmpty()) {
                    appliedToUser = true;
                } else {
                    appliedToUser = userIds.contains(user.getId());
                }
            } catch (Exception e) {
                log.error("解析用户ID列表失败", e);
                appliedToUser = false;
            }
            
            // 如果不对该用户生效，返回禁用状态
            if (!appliedToUser) {
                config.put("enabled", false);
            }
            
            // 移除用户ID列表（前端不需要知道）
            config.remove("userIds");
            
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取用户全局裁剪配置失败", e);
            return Result.error(500, "获取配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/admin/system-config/global-crop")
    @Operation(summary = "更新全局裁剪配置", description = "更新全局裁剪配置，可指定对哪些用户生效")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateGlobalCropConfig(
            @RequestParam(required = false) Integer cropX,
            @RequestParam(required = false) Integer cropY,
            @RequestParam(required = false) Integer cropWidth,
            @RequestParam(required = false) Integer cropHeight,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String userIds) {
        
        log.info("更新全局裁剪配置请求: enabled={}, x={}, y={}, width={}, height={}, userIds={}", 
                 enabled, cropX, cropY, cropWidth, cropHeight, userIds);
        
        systemConfigService.updateGlobalCropConfig(cropX, cropY, cropWidth, cropHeight, enabled);
        
        // 保存用户权限配置
        if (userIds != null) {
            systemConfigService.setConfigValue("global_crop_user_ids", userIds);
        }
        
        return Result.<Void>success("全局裁剪配置更新成功", null);
    }
}

