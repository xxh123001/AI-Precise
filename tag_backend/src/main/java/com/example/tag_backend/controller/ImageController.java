package com.example.tag_backend.controller;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.common.result.Result;
import com.example.tag_backend.dto.request.BatchUpdateLabelConfigRequest;
import com.example.tag_backend.dto.request.ImageAssignRequest;
import com.example.tag_backend.dto.request.ImageQueryRequest;
import com.example.tag_backend.dto.request.ImageUploadRequest;
import com.example.tag_backend.dto.response.ImageResponse;
import com.example.tag_backend.entity.Annotation;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.repository.AnnotationRepository;
import com.example.tag_backend.service.ImageService;
import com.example.tag_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图像管理控制器
 * 处理图像相关的HTTP请求
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/admin/images")
@Tag(name = "图像管理", description = "图像上传、下载、分配等相关接口")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageService imageService;
    private final UserService userService;
    private final AnnotationRepository annotationRepository;

    @PostMapping("/upload")
    @Operation(summary = "上传图像", description = "上传单个图像文件")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "标签配置ID") @RequestParam(value = "labelConfigId", required = false) String labelConfigIdStr) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Long uploadBy = userService.getUserEntityByUsername(username).getId();

        // 转换 labelConfigId 参数，处理 "null" 字符串和空值情况
        Long labelConfigId = parseLabelConfigId(labelConfigIdStr);

        ImageUploadRequest request = new ImageUploadRequest();
        request.setLabelConfigId(labelConfigId);

        log.info("上传图像请求，文件: {}, 标签配置ID: {}, 上传者: {}", 
                file.getOriginalFilename(), labelConfigId, username);
        ImageResponse response = imageService.uploadImage(file, request, uploadBy);
        return Result.success("图像上传成功", response);
    }

    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传图像", description = "批量上传多个图像文件")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ImageResponse>> batchUploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "标签配置ID") @RequestParam(value = "labelConfigId", required = false) String labelConfigIdStr) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Long uploadBy = userService.getUserEntityByUsername(username).getId();

        // 转换 labelConfigId 参数，处理 "null" 字符串和空值情况
        Long labelConfigId = parseLabelConfigId(labelConfigIdStr);

        ImageUploadRequest request = new ImageUploadRequest();
        request.setLabelConfigId(labelConfigId);

        log.info("批量上传图像请求，文件数量: {}, 标签配置ID: {}, 上传者: {}", 
                files.size(), labelConfigId, username);
        List<ImageResponse> responses = imageService.batchUploadImages(files, request, uploadBy);
        return Result.success("批量上传完成", responses);
    }

    @GetMapping
    @Operation(summary = "获取图像列表", description = "分页获取图像列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<ImageResponse>> getImageList(
            @Valid @ParameterObject ImageQueryRequest request) {
        log.info("获取图像列表请求: {}", request);
        PageResponse<ImageResponse> response = imageService.getImageList(request);
        return Result.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取图像详情", description = "根据ID获取图像详细信息")
    public Result<ImageResponse> getImageById(
            @Parameter(description = "图像ID") @PathVariable Long id) {
        log.info("获取图像详情请求，ID: {}", id);
        ImageResponse response = imageService.getImageById(id);
        return Result.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图像", description = "删除指定图像")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteImage(
            @Parameter(description = "图像ID") @PathVariable Long id) {
        log.info("删除图像请求，ID: {}", id);
        imageService.deleteImage(id);
        return Result.<Void>success("图像删除成功", null);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除图像", description = "批量删除多个图像")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDeleteImages(
            @RequestBody List<Long> ids) {
        log.info("批量删除图像请求，ID列表: {}", ids);
        imageService.batchDeleteImages(ids);
        return Result.<Void>success("批量删除完成", null);
    }

    @PostMapping("/assign")
    @Operation(summary = "分配图像", description = "将图像分配给用户进行标注")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignImages(
            @Valid @RequestBody ImageAssignRequest request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Long assignBy = userService.getUserEntityByUsername(username).getId();

        log.info("分配图像请求，图像数量: {}, 用户数量: {}, 分配者: {}", 
                request.getImageIds().size(), request.getUserIds().size(), username);
        
        imageService.assignImages(request, assignBy);
        return Result.<Void>success("图像分配成功", null);
    }

    @PutMapping("/{id}/label-config")
    @Operation(summary = "更新图像标签配置", description = "更新图像关联的标签配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateImageLabelConfig(
            @Parameter(description = "图像ID") @PathVariable Long id,
            @RequestParam(value = "labelConfigId", required = false) String labelConfigIdStr) {
        
        // 处理标签配置ID参数
        Long labelConfigId = parseLabelConfigId(labelConfigIdStr);
        
        log.info("更新图像标签配置请求，图像ID: {}, 标签配置ID: {}", id, labelConfigId);
        imageService.updateImageLabelConfig(id, labelConfigId);
        return Result.<Void>success("标签配置更新成功", null);
    }

    @PutMapping("/batch/label-config")
    @Operation(summary = "批量更新图像标签配置", description = "批量更新多个图像的标签配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchUpdateImageLabelConfig(
            @RequestBody BatchUpdateLabelConfigRequest request) {
        
        log.info("批量更新图像标签配置请求，图像数量: {}, 标签配置ID: {}", 
                request.getImageIds().size(), request.getLabelConfigId());
        
        imageService.batchUpdateImageLabelConfig(request.getImageIds(), request.getLabelConfigId());
        return Result.<Void>success("批量标签配置更新成功", null);
    }

    @GetMapping("/assigned")
    @Operation(summary = "获取分配的图像", description = "获取当前用户分配的图像列表")
    public Result<PageResponse<ImageResponse>> getAssignedImages(
            @Valid @ParameterObject ImageQueryRequest request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Long userId = userService.getUserEntityByUsername(username).getId();

        log.info("获取分配图像请求，用户: {}", username);
        PageResponse<ImageResponse> response = imageService.getAssignedImages(userId, request);
        return Result.success(response);
    }

    @GetMapping("/{imageId}/annotations")
    @Operation(summary = "获取图像的所有标注", description = "管理员查看指定图像的所有标注结果")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Object> getImageAnnotations(@PathVariable Long imageId) {
        log.info("管理员获取图像 {} 的所有标注结果", imageId);
        
        try {
            // 获取该图像的所有标注记录
            List<Annotation> annotations = annotationRepository.findByImageId(imageId);
            
            if (annotations.isEmpty()) {
                return Result.success("该图像暂无标注记录", Map.of("image", imageService.getImageById(imageId), "annotations", List.of()));
            }
            
            // 转换为响应格式
            List<Object> result = annotations.stream().map(annotation -> {
                Map<String, Object> annotationData = new HashMap<>();
                annotationData.put("id", annotation.getId());
                annotationData.put("imageId", annotation.getImageId());
                annotationData.put("userId", annotation.getUserId());
                annotationData.put("status", annotation.getStatus());
                annotationData.put("labels", annotation.getLabels());
                annotationData.put("createdAt", annotation.getCreatedAt());
                annotationData.put("updatedAt", annotation.getUpdatedAt());
                
                // 获取用户信息
                try {
                    User user = userService.getUserEntityById(annotation.getUserId());
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    annotationData.put("user", userInfo);
                } catch (Exception e) {
                    log.warn("获取用户信息失败，userId: {}", annotation.getUserId());
                    annotationData.put("user", Map.of("id", annotation.getUserId(), "username", "未知用户"));
                }
                
                return annotationData;
            }).collect(Collectors.toList());
            
            // 同时获取图像信息
            ImageResponse imageInfo = imageService.getImageById(imageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("image", imageInfo);
            response.put("annotations", result);
            
            log.info("成功获取图像 {} 的 {} 条标注记录", imageId, result.size());
            return Result.success("获取成功", response);
            
        } catch (Exception e) {
            log.error("获取图像标注结果失败", e);
            return Result.error(500, "获取标注结果失败");
        }
    }

    /**
     * 解析标签配置ID参数
     * 处理前端发送的 "null" 字符串或空值情况
     * 
     * @param labelConfigIdStr 字符串形式的标签配置ID
     * @return 解析后的Long类型ID，无效值返回null
     */
    private Long parseLabelConfigId(String labelConfigIdStr) {
        if (labelConfigIdStr == null || labelConfigIdStr.trim().isEmpty() || "null".equalsIgnoreCase(labelConfigIdStr.trim())) {
            return null;
        }
        
        try {
            return Long.valueOf(labelConfigIdStr.trim());
        } catch (NumberFormatException e) {
            log.warn("无效的标签配置ID参数: {}", labelConfigIdStr);
            return null;
        }
    }
}
