package com.example.tag_backend.service;

import com.example.tag_backend.dto.request.AnnotationFilterRequest;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

/**
 * 标注筛选服务接口
 * 提供标注数据筛选和导出功能
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-13
 */
public interface AnnotationFilterService {

    /**
     * 搜索标注数据
     * 
     * @param request 筛选请求
     * @return 搜索结果
     */
    Map<String, Object> searchAnnotations(AnnotationFilterRequest request);

    /**
     * 筛选双标签数据
     * 筛选同时包含多个指定标签的标注数据
     * 
     * @param request 筛选请求
     * @return 筛选结果
     */
    Map<String, Object> filterDoubleTags(AnnotationFilterRequest request);

    /**
     * 导出图像文件
     * 将筛选出的图像文件打包为ZIP文件
     * 
     * @param request 筛选请求
     * @return ZIP文件资源
     */
    Resource exportImages(AnnotationFilterRequest request);

    /**
     * 获取有标注记录的用户列表
     * 
     * @return 用户列表
     */
    List<Map<String, Object>> getAnnotationUsers();

    /**
     * 获取常见标签列表
     * 从所有标注中提取最常用的标签
     * 
     * @return 标签列表
     */
    List<String> getCommonTags();

    /**
     * 预览筛选结果
     * 返回筛选结果的前几条数据，用于确认筛选条件
     * 
     * @param request 筛选请求
     * @return 预览数据
     */
    Map<String, Object> previewFilterResults(AnnotationFilterRequest request);

    /**
     * 统计筛选结果
     * 返回筛选条件匹配的记录数量统计
     * 
     * @param request 筛选请求
     * @return 统计数据
     */
    Map<String, Object> getFilterStatistics(AnnotationFilterRequest request);
}

