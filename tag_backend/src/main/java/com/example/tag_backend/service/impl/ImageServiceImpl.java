package com.example.tag_backend.service.impl;

import com.example.tag_backend.common.result.PageResponse;
import com.example.tag_backend.dto.request.ImageAssignRequest;
import com.example.tag_backend.dto.request.ImageQueryRequest;
import com.example.tag_backend.dto.request.ImageUploadRequest;
import com.example.tag_backend.dto.response.ImageResponse;
import com.example.tag_backend.entity.Image;
import com.example.tag_backend.entity.ImageAssignment;
import com.example.tag_backend.entity.LabelConfig;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.AssignmentStatus;
import com.example.tag_backend.exception.BusinessException;
import com.example.tag_backend.repository.*;
import com.example.tag_backend.service.FileStorageService;
import com.example.tag_backend.service.ImageService;
import com.example.tag_backend.service.LabelConfigService;
import com.example.tag_backend.service.CsvDataService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图像服务实现类
 * 负责处理图像相关的业务逻辑
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final ImageAssignmentRepository imageAssignmentRepository;
    private final LabelConfigRepository labelConfigRepository;
    private final UserRepository userRepository;
    private final AnnotationRepository annotationRepository;
    private final FileStorageService fileStorageService;
    private final LabelConfigService labelConfigService;
    private final CsvDataService csvDataService;

    @Override
    @Transactional
    public ImageResponse uploadImage(MultipartFile file, ImageUploadRequest request, Long uploadBy) {
        log.info("开始上传图像，文件名: {}, 上传者: {}", file.getOriginalFilename(), uploadBy);

        // 验证标签配置（如果提供）
        validateLabelConfig(request.getLabelConfigId());

        // 存储文件
        String filename = fileStorageService.storeFile(file);

        // 创建图像记录
        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalName(file.getOriginalFilename());
        image.setFilePath(fileStorageService.getFileUrl(filename));
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        image.setUploadBy(uploadBy);
        image.setLabelConfigId(request.getLabelConfigId());

        // 查找并设置CSV相关数据
        setCsvDataToImage(image, filename);

        image = imageRepository.save(image);

        log.info("图像上传成功，图像ID: {}", image.getId());

        return convertToResponse(image);
    }

    @Override
    @Transactional
    public List<ImageResponse> batchUploadImages(List<MultipartFile> files, ImageUploadRequest request, Long uploadBy) {
        log.info("开始批量上传图像，文件数量: {}, 上传者: {}", files.size(), uploadBy);

        // 验证标签配置（如果提供）
        validateLabelConfig(request.getLabelConfigId());

        List<ImageResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                ImageResponse response = uploadImage(file, request, uploadBy);
                responses.add(response);
            } catch (Exception e) {
                log.error("批量上传中文件上传失败: {}", file.getOriginalFilename(), e);
                // 继续处理下一个文件，不中断整个批量上传过程
            }
        }

        log.info("批量上传完成，成功数量: {}/{}", responses.size(), files.size());

        return responses;
    }

    @Override
    public ImageResponse getImageById(Long id) {
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new BusinessException("图像不存在"));
        return convertToResponse(image, true);  // 包含完整的标签配置
    }

    @Override
    public PageResponse<ImageResponse> getImageList(ImageQueryRequest request) {
        log.debug("查询图像列表，参数: {}", request);

        // 构建分页参数
        Pageable pageable = buildPageable(request);
        
        Page<Image> imagePage;
        
        // 优先处理分配状态筛选
        if (request.getIsAssigned() != null) {
            log.info("🔍 使用分配状态筛选: isAssigned = {}", request.getIsAssigned());
            if (request.getIsAssigned()) {
                log.info("📋 查询已分配图像");
                imagePage = imageRepository.findAssignedImages(pageable);
            } else {
                log.info("📋 查询未分配图像");
                imagePage = imageRepository.findUnassignedImages(pageable);
            }
        } else {
            // 使用原有的Specification查询
            Specification<Image> spec = buildImageSpecification(request);
            imagePage = imageRepository.findAll(spec, pageable);
        }

        // 转换结果
        List<ImageResponse> imageResponses = imagePage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResponse.of(imageResponses, imagePage);
    }

    @Override
    @Transactional
    public void deleteImage(Long id) {
        log.info("删除图像开始，图像ID: {}", id);

        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new BusinessException("图像不存在"));

        // 删除文件
        try {
            fileStorageService.deleteFile(image.getFilename());
        } catch (Exception e) {
            log.warn("删除图像文件失败: {}", image.getFilename(), e);
        }

        // 删除数据库记录
        imageRepository.delete(image);

        log.info("图像删除成功，图像ID: {}", id);
    }

    @Override
    @Transactional
    public void batchDeleteImages(List<Long> ids) {
        log.info("批量删除图像开始，ID列表: {}", ids);

        for (Long id : ids) {
            try {
                deleteImage(id);
            } catch (Exception e) {
                log.error("批量删除中图像删除失败，ID: {}", id, e);
            }
        }

        log.info("批量删除图像完成");
    }

    @Override
    @Transactional
    public void assignImages(ImageAssignRequest request, Long assignBy) {
        log.info("分配图像开始，图像数量: {}, 用户数量: {}, 分配者: {}", 
                request.getImageIds().size(), request.getUserIds().size(), assignBy);

        // 验证图像是否存在
        List<Image> images = imageRepository.findAllById(request.getImageIds());
        if (images.size() != request.getImageIds().size()) {
            throw new BusinessException("部分图像不存在");
        }

        // 验证用户是否存在
        List<User> users = userRepository.findAllById(request.getUserIds());
        if (users.size() != request.getUserIds().size()) {
            throw new BusinessException("部分用户不存在");
        }

        // 创建分配记录
        List<ImageAssignment> assignments = new ArrayList<>();
        for (Long imageId : request.getImageIds()) {
            for (Long userId : request.getUserIds()) {
                // 检查是否已存在分配记录
                if (!imageAssignmentRepository.existsByImageIdAndUserId(imageId, userId)) {
                    ImageAssignment assignment = new ImageAssignment();
                    assignment.setImageId(imageId);
                    assignment.setUserId(userId);
                    assignment.setStatus(AssignmentStatus.ASSIGNED);
                    assignment.setAssignedBy(assignBy);
                    assignments.add(assignment);
                }
            }
        }

        imageAssignmentRepository.saveAll(assignments);

        log.info("图像分配完成，创建分配记录数: {}", assignments.size());
    }

    @Override
    public PageResponse<ImageResponse> getAssignedImages(Long userId, ImageQueryRequest request) {
        log.debug("查询用户分配的图像，用户ID: {}", userId);

        // 构建分页参数
        Pageable pageable = buildPageable(request);

        // 查询分配的图像
        Page<Image> imagePage = imageRepository.findAssignedImagesByUserId(userId, pageable);

        // 转换结果
        List<ImageResponse> imageResponses = imagePage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return PageResponse.of(imageResponses, imagePage);
    }

    @Override
    @Transactional
    public void updateImageLabelConfig(Long id, Long labelConfigId) {
        log.info("更新图像标签配置，图像ID: {}, 标签配置ID: {}", id, labelConfigId);

        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new BusinessException("图像不存在"));

        // 验证标签配置
        validateLabelConfig(labelConfigId);

        image.setLabelConfigId(labelConfigId);
        imageRepository.save(image);

        log.info("图像标签配置更新成功");
    }

    @Override
    @Transactional
    public void batchUpdateImageLabelConfig(List<Long> imageIds, Long labelConfigId) {
        log.info("批量更新图像标签配置，图像数量: {}, 标签配置ID: {}", imageIds.size(), labelConfigId);

        // 验证标签配置
        validateLabelConfig(labelConfigId);

        // 查找所有图像
        List<Image> images = imageRepository.findAllById(imageIds);
        if (images.size() != imageIds.size()) {
            throw new BusinessException("部分图像不存在");
        }

        // 更新标签配置
        images.forEach(image -> image.setLabelConfigId(labelConfigId));
        imageRepository.saveAll(images);

        log.info("批量更新图像标签配置完成，成功更新数量: {}", images.size());
    }

    /**
     * 验证标签配置是否存在且有效
     */
    private void validateLabelConfig(Long labelConfigId) {
        if (labelConfigId != null) {
            LabelConfig labelConfig = labelConfigRepository.findById(labelConfigId)
                .orElseThrow(() -> new BusinessException("标签配置不存在"));
            
            if (!labelConfig.getIsActive()) {
                throw new BusinessException("标签配置已禁用");
            }
        }
    }

    /**
     * 构建图像查询条件
     */
    private Specification<Image> buildImageSpecification(ImageQueryRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ID精确查询
            if (request.getId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), request.getId()));
            }

            // 关键字模糊查询（文件名或原始名称）
            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                // 直接对字符串进行LIKE匹配，避免使用lower()函数造成类型冲突
                Expression<String> filenameExpr = root.get("filename").as(String.class);
                Expression<String> originalNameExpr = root.get("originalName").as(String.class);
                Predicate filenamePredicate = criteriaBuilder.like(filenameExpr, keyword);
                Predicate originalNamePredicate = criteriaBuilder.like(originalNameExpr, keyword);
                predicates.add(criteriaBuilder.or(filenamePredicate, originalNamePredicate));
            }

            // 原始文件名模糊查询（保持向后兼容）
            if (StringUtils.hasText(request.getOriginalName())) {
                // 直接对字符串进行LIKE匹配，避免使用lower()函数造成类型冲突
                Expression<String> originalNameExpr = root.get("originalName").as(String.class);
                predicates.add(criteriaBuilder.like(
                    originalNameExpr,
                    "%" + request.getOriginalName().toLowerCase() + "%"
                ));
            }

            // 上传者筛选
            if (request.getUploadBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("uploadBy"), request.getUploadBy()));
            }

            // 标签配置筛选
            if (request.getLabelConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("labelConfigId"), request.getLabelConfigId()));
            }

            // 分配状态筛选
            if (request.getIsAssigned() != null) {
                log.info("🔍 分配状态筛选条件: isAssigned = {}", request.getIsAssigned());
                
                // 使用EXISTS子查询来检查分配状态
                var subquery = query.subquery(ImageAssignment.class);
                var subRoot = subquery.from(ImageAssignment.class);
                subquery.select(subRoot);
                subquery.where(criteriaBuilder.equal(subRoot.get("image").get("id"), root.get("id")));
                
                if (request.getIsAssigned()) {
                    // 已分配：存在分配记录
                    log.info("📋 使用EXISTS子查询筛选已分配图像");
                    predicates.add(criteriaBuilder.exists(subquery));
                } else {
                    // 未分配：不存在分配记录
                    log.info("📋 使用NOT EXISTS子查询筛选未分配图像");
                    predicates.add(criteriaBuilder.not(criteriaBuilder.exists(subquery)));
                }
            }

            // 分配用户ID筛选
            if (request.getAssignedUserId() != null) {
                var assignmentJoin = root.join("imageAssignments", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(criteriaBuilder.equal(assignmentJoin.get("userId"), request.getAssignedUserId()));
            }

            // 分配用户名筛选
            if (StringUtils.hasText(request.getAssignedUsername())) {
                var assignmentJoin = root.join("imageAssignments", jakarta.persistence.criteria.JoinType.INNER);
                var userJoin = assignmentJoin.join("user", jakarta.persistence.criteria.JoinType.INNER);
                // 直接对字符串进行LIKE匹配，避免使用lower()函数造成类型冲突
                Expression<String> usernameExpr = userJoin.get("username").as(String.class);
                predicates.add(criteriaBuilder.like(
                    usernameExpr,
                    "%" + request.getAssignedUsername().toLowerCase() + "%"
                ));
            }

            // 避免重复结果（当连接分配表时）
            if (request.getAssignedUserId() != null || 
                StringUtils.hasText(request.getAssignedUsername())) {
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构建分页参数
     */
    private Pageable buildPageable(ImageQueryRequest request) {
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
            Math.max(0, request.getPage()),
            Math.max(1, request.getSize()),
            sort
        );
    }

    /**
     * 设置CSV相关数据到图像实体
     * @param image 图像实体
     * @param filename 文件名
     */
    private void setCsvDataToImage(Image image, String filename) {
        try {
            // 提取不带扩展名的文件名
            String baseFilename = filename;
            int lastDotIndex = filename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                baseFilename = filename.substring(0, lastDotIndex);
            }
            
            log.debug("查找CSV数据，文件名: {}", baseFilename);
            
            // 查找CSV数据
            CsvDataService.CsvRowData csvData = csvDataService.findByFilename(baseFilename);
            
            if (csvData != null) {
                log.info("找到CSV数据，文件名: {}", baseFilename);
                
                // 设置CSV相关字段
                image.setFolder(csvData.getFolder());
                image.setReid(csvData.getReid());
                image.setImageClass(csvData.getImageClass());
                image.setFirstTrainLabel(csvData.getFirstTrainLabel());
                image.setTestLabel(csvData.getTestLabel());
                image.setModel1(csvData.getModel1());
                image.setPasConf(csvData.getPasConf());
                image.setIfLabel(csvData.getIfLabel());
                image.setPredictLabel(csvData.getPredictLabel());
                image.setPredictConfidence(csvData.getPredictConfidence());
            } else {
                log.warn("未找到CSV数据，文件名: {}", baseFilename);
            }
        } catch (Exception e) {
            log.error("设置CSV数据时出错，文件名: {}", filename, e);
            // 即使CSV数据设置失败，也不影响图像上传
        }
    }

    /**
     * 转换为响应DTO
     */
    private ImageResponse convertToResponse(Image image) {
        return convertToResponse(image, false);
    }

    /**
     * 转换为响应DTO（支持包含完整标签配置）
     */
    private ImageResponse convertToResponse(Image image, boolean includeLabelConfig) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setFilename(image.getFilename());
        response.setOriginalName(image.getOriginalName());
        response.setUrl(image.getFilePath());
        response.setFileSize(image.getFileSize());
        response.setMimeType(image.getMimeType());
        response.setUploadBy(image.getUploadBy());
        response.setLabelConfigId(image.getLabelConfigId());
        response.setCreatedAt(image.getCreatedAt());

        // 设置CSV相关字段
        response.setFolder(image.getFolder());
        response.setReid(image.getReid());
        response.setImageClass(image.getImageClass());
        response.setFirstTrainLabel(image.getFirstTrainLabel());
        response.setTestLabel(image.getTestLabel());
        response.setModel1(image.getModel1());
        response.setPasConf(image.getPasConf());
        response.setIfLabel(image.getIfLabel());
        response.setPredictLabel(image.getPredictLabel());
        response.setPredictConfidence(image.getPredictConfidence());

        // 设置上传者用户名（如果关联加载）
        if (image.getUploader() != null) {
            response.setUploaderUsername(image.getUploader().getUsername());
        }

        // 设置标签配置信息
        if (image.getLabelConfigId() != null) {
            // 设置标签配置名称（如果关联加载）
            if (image.getLabelConfig() != null) {
                response.setLabelConfigName(image.getLabelConfig().getName());
            }
            
            // 如果需要完整的标签配置，则加载它
            if (includeLabelConfig) {
                try {
                    response.setLabelConfig(labelConfigService.getLabelConfigById(image.getLabelConfigId()));
                } catch (Exception e) {
                    log.warn("获取标签配置失败，配置ID: {}", image.getLabelConfigId(), e);
                }
            }
        }

        // 检查是否已分配和已标注，获取分配用户列表
        List<ImageAssignment> assignments = imageAssignmentRepository.findByImageId(image.getId());
        response.setIsAssigned(assignments.size() > 0);
        
        // 设置分配用户列表和用户ID列表
        if (!assignments.isEmpty()) {
            List<String> assignedUsernames = new java.util.ArrayList<>();
            List<Long> assignedUserIds = new java.util.ArrayList<>();
            
            assignments.stream()
                .map(ImageAssignment::getUser)
                .filter(user -> user != null)
                .forEach(user -> {
                    String username = user.getUsername();
                    Long userId = user.getId();
                    
                    // 去重添加
                    if (!assignedUsernames.contains(username)) {
                        assignedUsernames.add(username);
                    }
                    if (!assignedUserIds.contains(userId)) {
                        assignedUserIds.add(userId);
                    }
                });
            
            response.setAssignedUsers(assignedUsernames);
            response.setAssignedUserIds(assignedUserIds);
        }
        
        response.setIsAnnotated(annotationRepository.findByImageId(image.getId()).size() > 0);

        return response;
    }
}
