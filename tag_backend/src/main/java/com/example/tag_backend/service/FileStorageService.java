package com.example.tag_backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 定义文件存储相关的业务操作
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface FileStorageService {

    /**
     * 存储文件
     * 
     * @param file 上传的文件
     * @return 存储的文件名
     */
    String storeFile(MultipartFile file);

    /**
     * 加载文件作为资源
     * 
     * @param filename 文件名
     * @return 文件资源
     */
    Resource loadFileAsResource(String filename);

    /**
     * 删除文件
     * 
     * @param filename 文件名
     */
    void deleteFile(String filename);

    /**
     * 获取文件访问URL
     * 
     * @param filename 文件名
     * @return 访问URL
     */
    String getFileUrl(String filename);

    /**
     * 验证文件类型
     * 
     * @param file 文件
     * @return 是否为有效的图像文件
     */
    boolean isValidImageFile(MultipartFile file);
}
