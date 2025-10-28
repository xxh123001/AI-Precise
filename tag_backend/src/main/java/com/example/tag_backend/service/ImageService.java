package com.example.tag_backend.service;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.dto.request.ImageAssignRequest;
import com.example.tag_backend.dto.request.ImageQueryRequest;
import com.example.tag_backend.dto.request.ImageUploadRequest;
import com.example.tag_backend.dto.response.ImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图像服务接口
 * 定义图像相关的业务操作
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface ImageService {

    /**
     * 上传单个图像
     * 
     * @param file 图像文件
     * @param request 上传请求参数
     * @param uploadBy 上传者ID
     * @return 图像信息
     */
    ImageResponse uploadImage(MultipartFile file, ImageUploadRequest request, Long uploadBy);

    /**
     * 批量上传图像
     * 
     * @param files 图像文件列表
     * @param request 上传请求参数
     * @param uploadBy 上传者ID
     * @return 图像信息列表
     */
    List<ImageResponse> batchUploadImages(List<MultipartFile> files, ImageUploadRequest request, Long uploadBy);

    /**
     * 根据ID获取图像信息
     * 
     * @param id 图像ID
     * @return 图像信息
     */
    ImageResponse getImageById(Long id);

    /**
     * 分页查询图像列表
     * 
     * @param request 查询参数
     * @return 分页图像列表
     */
    PageResponse<ImageResponse> getImageList(ImageQueryRequest request);

    /**
     * 删除图像
     * 
     * @param id 图像ID
     */
    void deleteImage(Long id);

    /**
     * 批量删除图像
     * 
     * @param ids 图像ID列表
     */
    void batchDeleteImages(List<Long> ids);

    /**
     * 分配图像给用户
     * 
     * @param request 分配请求
     * @param assignBy 分配者ID
     */
    void assignImages(ImageAssignRequest request, Long assignBy);

    /**
     * 获取用户分配的图像列表
     * 
     * @param userId 用户ID
     * @param request 查询参数
     * @return 分页图像列表
     */
    PageResponse<ImageResponse> getAssignedImages(Long userId, ImageQueryRequest request);

    /**
     * 更新图像的标签配置
     * 
     * @param id 图像ID
     * @param labelConfigId 标签配置ID
     */
    void updateImageLabelConfig(Long id, Long labelConfigId);

    /**
     * 批量更新图像的标签配置
     * 
     * @param imageIds 图像ID列表
     * @param labelConfigId 标签配置ID
     */
    void batchUpdateImageLabelConfig(List<Long> imageIds, Long labelConfigId);
}
