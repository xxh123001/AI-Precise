package com.example.tag_backend.service;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.dto.request.CreateLabelConfigRequest;
import com.example.tag_backend.dto.request.LabelConfigQueryRequest;
import com.example.tag_backend.dto.response.LabelConfigResponse;

import java.util.List;

/**
 * 标签配置服务接口
 * 定义标签配置相关的业务操作
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
public interface LabelConfigService {

    /**
     * 创建标签配置
     * 
     * @param request 创建请求
     * @param createdBy 创建者ID
     * @return 标签配置信息
     */
    LabelConfigResponse createLabelConfig(CreateLabelConfigRequest request, Long createdBy);

    /**
     * 根据ID获取标签配置
     * 
     * @param id 配置ID
     * @return 标签配置信息
     */
    LabelConfigResponse getLabelConfigById(Long id);

    /**
     * 分页查询标签配置列表
     * 
     * @param request 查询参数
     * @return 分页标签配置列表
     */
    PageResponse<LabelConfigResponse> getLabelConfigList(LabelConfigQueryRequest request);

    /**
     * 获取所有激活的标签配置
     * 
     * @return 激活的标签配置列表
     */
    List<LabelConfigResponse> getActiveLabelConfigs();

    /**
     * 更新标签配置
     * 
     * @param id 配置ID
     * @param request 更新请求
     * @return 更新后的标签配置信息
     */
    LabelConfigResponse updateLabelConfig(Long id, CreateLabelConfigRequest request);

    /**
     * 删除标签配置
     * 
     * @param id 配置ID
     */
    void deleteLabelConfig(Long id);

    /**
     * 启用标签配置
     * 
     * @param id 配置ID
     */
    void enableLabelConfig(Long id);

    /**
     * 禁用标签配置
     * 
     * @param id 配置ID
     */
    void disableLabelConfig(Long id);

    /**
     * 验证标签配置JSON格式
     * 
     * @param configJson 配置JSON字符串
     * @return 是否有效
     */
    boolean validateLabelConfigJson(String configJson);
}
