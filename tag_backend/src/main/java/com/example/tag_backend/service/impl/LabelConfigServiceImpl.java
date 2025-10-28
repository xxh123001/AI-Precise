package com.example.tag_backend.service.impl;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.dto.request.CreateLabelConfigRequest;
import com.example.tag_backend.dto.request.LabelConfigQueryRequest;
import com.example.tag_backend.dto.response.LabelConfigResponse;
import com.example.tag_backend.entity.LabelConfig;
import com.example.tag_backend.exception.BusinessException;
import com.example.tag_backend.repository.ImageRepository;
import com.example.tag_backend.repository.LabelConfigRepository;
import com.example.tag_backend.service.LabelConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签配置服务实现类
 * 负责处理标签配置相关的业务逻辑
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LabelConfigServiceImpl implements LabelConfigService {

    private final LabelConfigRepository labelConfigRepository;
    private final ImageRepository imageRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LabelConfigResponse createLabelConfig(CreateLabelConfigRequest request, Long createdBy) {
        log.info("创建标签配置开始，名称: {}, 创建者: {}", request.getName(), createdBy);

        // 检查名称是否已存在
        if (labelConfigRepository.existsByName(request.getName())) {
            throw new BusinessException("标签配置名称已存在");
        }

        // 验证JSON格式
        if (!validateLabelConfigJson(request.getConfig())) {
            throw new BusinessException("标签配置JSON格式无效");
        }

        // 创建标签配置实体
        LabelConfig labelConfig = new LabelConfig();
        labelConfig.setName(request.getName());
        labelConfig.setDescription(request.getDescription());
        labelConfig.setConfig(request.getConfig());
        labelConfig.setIsActive(request.getIsActive());
        labelConfig.setCreatedBy(createdBy);

        labelConfig = labelConfigRepository.save(labelConfig);

        log.info("标签配置创建成功，配置ID: {}", labelConfig.getId());

        return convertToResponse(labelConfig);
    }

    @Override
    @Cacheable(value = "labelConfigs", key = "#id")
    public LabelConfigResponse getLabelConfigById(Long id) {
        log.debug("从数据库查询标签配置，ID: {}", id);
        LabelConfig labelConfig = labelConfigRepository.findById(id)
            .orElseThrow(() -> new BusinessException("标签配置不存在"));
        return convertToResponse(labelConfig);
    }

    @Override
    public PageResponse<LabelConfigResponse> getLabelConfigList(LabelConfigQueryRequest request) {
        log.debug("查询标签配置列表，参数: {}", request);

        // 构建查询条件
        Specification<LabelConfig> spec = buildLabelConfigSpecification(request);

        // 构建分页参数
        Pageable pageable = buildPageable(request);

        // 执行查询
        Page<LabelConfig> labelConfigPage = labelConfigRepository.findAll(spec, pageable);

        // 转换结果
        List<LabelConfigResponse> responses = labelConfigPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, labelConfigPage);
    }

    @Override
    @Cacheable(value = "activeLabelConfigs", key = "'all'")
    public List<LabelConfigResponse> getActiveLabelConfigs() {
        log.debug("从数据库获取所有激活的标签配置");

        List<LabelConfig> labelConfigs = labelConfigRepository.findByIsActiveTrue();
        return labelConfigs.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CachePut(value = "labelConfigs", key = "#id")
    @CacheEvict(value = "activeLabelConfigs", allEntries = true)
    public LabelConfigResponse updateLabelConfig(Long id, CreateLabelConfigRequest request) {
        log.info("更新标签配置开始，配置ID: {}", id);

        LabelConfig labelConfig = labelConfigRepository.findById(id)
            .orElseThrow(() -> new BusinessException("标签配置不存在"));

        // 检查名称是否已被其他配置使用
        if (!labelConfig.getName().equals(request.getName()) && 
            labelConfigRepository.existsByName(request.getName())) {
            throw new BusinessException("标签配置名称已存在");
        }

        // 验证JSON格式
        if (!validateLabelConfigJson(request.getConfig())) {
            throw new BusinessException("标签配置JSON格式无效");
        }

        // 更新配置信息
        labelConfig.setName(request.getName());
        labelConfig.setDescription(request.getDescription());
        labelConfig.setConfig(request.getConfig());
        labelConfig.setIsActive(request.getIsActive());

        labelConfig = labelConfigRepository.save(labelConfig);

        log.info("标签配置更新成功，配置ID: {}，已清除相关缓存", labelConfig.getId());

        return convertToResponse(labelConfig);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"labelConfigs", "activeLabelConfigs"}, allEntries = true)
    public void deleteLabelConfig(Long id) {
        log.info("删除标签配置开始，配置ID: {}", id);

        LabelConfig labelConfig = labelConfigRepository.findById(id)
            .orElseThrow(() -> new BusinessException("标签配置不存在"));

        // 检查是否有图像使用此配置
        long imageCount = imageRepository.countByLabelConfigId(id);
        if (imageCount > 0) {
            throw new BusinessException("无法删除，仍有 " + imageCount + " 张图像使用此标签配置");
        }

        labelConfigRepository.delete(labelConfig);

        log.info("标签配置删除成功，配置ID: {}，已清除相关缓存", id);
    }

    @Override
    @Transactional
    public void enableLabelConfig(Long id) {
        updateLabelConfigStatus(id, true);
    }

    @Override
    @Transactional
    public void disableLabelConfig(Long id) {
        updateLabelConfigStatus(id, false);
    }

    @Override
    public boolean validateLabelConfigJson(String configJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(configJson);
            
            // 检查必要的字段
            if (!rootNode.has("version")) {
                log.warn("标签配置JSON缺少version字段");
                return false;
            }
            
            if (!rootNode.has("categories") || !rootNode.get("categories").isArray()) {
                log.warn("标签配置JSON缺少categories字段或不是数组");
                return false;
            }

            // 验证categories数组
            JsonNode categories = rootNode.get("categories");
            if (categories.size() == 0) {
                log.warn("标签配置JSON的categories数组为空");
                return false;
            }

            // 验证每个category的结构
            for (JsonNode category : categories) {
                if (!category.has("id") || !category.has("name") || !category.has("type")) {
                    log.warn("标签配置JSON的category缺少必要字段");
                    return false;
                }
                
                if (!category.has("options") || !category.get("options").isArray()) {
                    log.warn("标签配置JSON的category缺少options字段或不是数组");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("标签配置JSON解析失败", e);
            return false;
        }
    }

    /**
     * 更新标签配置状态
     */
    @CacheEvict(value = {"labelConfigs", "activeLabelConfigs"}, allEntries = true)
    private void updateLabelConfigStatus(Long id, Boolean isActive) {
        log.info("更新标签配置状态，配置ID: {}, 状态: {}", id, isActive);

        LabelConfig labelConfig = labelConfigRepository.findById(id)
            .orElseThrow(() -> new BusinessException("标签配置不存在"));

        labelConfig.setIsActive(isActive);
        labelConfigRepository.save(labelConfig);

        log.info("标签配置状态更新成功，配置ID: {}，已清除相关缓存", id);
    }

    /**
     * 构建标签配置查询条件
     */
    private Specification<LabelConfig> buildLabelConfigSpecification(LabelConfigQueryRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 名称模糊查询
            if (StringUtils.hasText(request.getName())) {
                // 直接对字符串进行LIKE匹配，避免使用lower()函数造成类型冲突
                Expression<String> nameExpr = root.get("name").as(String.class);
                predicates.add(criteriaBuilder.like(
                    nameExpr,
                    "%" + request.getName().toLowerCase() + "%"
                ));
            }

            // 激活状态筛选
            if (request.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), request.getIsActive()));
            }

            // 创建者筛选
            if (request.getCreatedBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy"), request.getCreatedBy()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构建分页参数
     */
    private Pageable buildPageable(LabelConfigQueryRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        
        if (StringUtils.hasText(request.getSort())) {
            String[] sortParts = request.getSort().split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = "asc".equalsIgnoreCase(sortParts[1]) 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, sortParts[0]);
            }
        }

        return PageRequest.of(
            Math.max(0, request.getPage() - 1),
            Math.max(1, request.getSize()),
            sort
        );
    }

    /**
     * 转换为响应DTO
     */
    private LabelConfigResponse convertToResponse(LabelConfig labelConfig) {
        LabelConfigResponse response = new LabelConfigResponse();
        response.setId(labelConfig.getId());
        response.setName(labelConfig.getName());
        response.setDescription(labelConfig.getDescription());
        response.setConfig(labelConfig.getConfig());
        response.setIsActive(labelConfig.getIsActive());
        response.setCreatedBy(labelConfig.getCreatedBy());
        response.setCreatedAt(labelConfig.getCreatedAt());
        response.setUpdatedAt(labelConfig.getUpdatedAt());

        // 设置创建者用户名（如果关联加载）
        if (labelConfig.getCreator() != null) {
            response.setCreatorUsername(labelConfig.getCreator().getUsername());
        }

        // 统计使用此配置的图像数量
        response.setImageCount(imageRepository.countByLabelConfigId(labelConfig.getId()));

        return response;
    }
}
