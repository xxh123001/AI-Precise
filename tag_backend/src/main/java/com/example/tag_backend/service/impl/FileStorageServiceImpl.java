package com.example.tag_backend.service.impl;

import com.example.tag_backend.exception.BusinessException;
import com.example.tag_backend.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 文件存储服务实现类
 * 负责文件的上传、下载、删除等操作
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    /**
     * 支持的图像文件类型
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    /**
     * 最大文件大小（50MB）
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public FileStorageServiceImpl(@Value("${app.file.upload-path}") String uploadPath) {
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new BusinessException("无法创建文件上传目录", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // 验证文件
        validateFile(file);

        // 生成唯一文件名
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + "." + extension;

        try {
            // 检查文件名是否包含无效字符
            if (filename.contains("..")) {
                throw new BusinessException("文件名包含无效路径序列: " + filename);
            }

            // 复制文件到目标位置
            Path targetLocation = this.fileStorageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件存储成功: {}", filename);
            return filename;

        } catch (IOException e) {
            log.error("文件存储失败: {}", originalFilename, e);
            throw new BusinessException("文件存储失败", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("文件不存在或无法读取: " + filename);
            }
        } catch (MalformedURLException e) {
            log.error("文件加载失败: {}", filename, e);
            throw new BusinessException("文件加载失败: " + filename, e);
        }
    }

    @Override
    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", filename);
        } catch (IOException e) {
            log.error("文件删除失败: {}", filename, e);
            throw new BusinessException("文件删除失败: " + filename, e);
        }
    }

    @Override
    public String getFileUrl(String filename) {
        return "/api/files/" + filename;
    }

    @Override
    public boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过50MB");
        }

        if (!isValidImageFile(file)) {
            throw new BusinessException("不支持的文件类型，仅支持图像文件");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";
    }
}
