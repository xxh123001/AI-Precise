package com.example.tag_backend.controller;

import com.example.tag_backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 文件控制器
 * 处理文件下载相关的HTTP请求
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件下载等相关接口")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{filename:.+}")
    @Operation(summary = "下载文件", description = "根据文件名下载文件，始终返回原始图片")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件名") @PathVariable String filename,
            HttpServletRequest request) {
        
        log.debug("文件下载请求，文件名: {}", filename);

        // 加载文件作为资源
        Resource resource = fileStorageService.loadFileAsResource(filename);

        // 尝试确定文件的内容类型
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            log.info("无法确定文件类型，使用默认类型");
        }

        // 如果无法确定内容类型，则设为默认值
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // 直接返回原始文件，不进行任何裁剪处理
        // 裁剪由前端通过CSS实现
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "inline; filename=\"" + resource.getFilename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(resource);
    }
}
