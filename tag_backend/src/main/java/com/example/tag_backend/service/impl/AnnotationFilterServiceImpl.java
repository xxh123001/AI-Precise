package com.example.tag_backend.service.impl;

import com.example.tag_backend.dto.request.AnnotationFilterRequest;
import com.example.tag_backend.entity.Annotation;
import com.example.tag_backend.entity.Image;
import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.AnnotationStatus;
import com.example.tag_backend.repository.AnnotationRepository;
import com.example.tag_backend.repository.UserRepository;
import com.example.tag_backend.service.AnnotationFilterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 标注筛选服务实现类
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnnotationFilterServiceImpl implements AnnotationFilterService {

    private final AnnotationRepository annotationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.file.upload-path:./uploads/}")
    private String uploadPath;

    @Override
    public Map<String, Object> searchAnnotations(AnnotationFilterRequest request) {
        log.info("执行标注数据搜索，请求参数: {}", request);

        try {
            // 构建查询条件
            Specification<Annotation> spec = buildSpecification(request);
            
            // 分页查询
            Pageable pageable = buildPageable(request);
            Page<Annotation> page = annotationRepository.findAll(spec, pageable);

            // 转换结果
            List<Map<String, Object>> results = page.getContent().stream()
                    .map(this::convertAnnotationToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("content", results);
            response.put("totalElements", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("currentPage", page.getNumber());
            response.put("pageSize", page.getSize());
            response.put("hasNext", page.hasNext());
            response.put("hasPrevious", page.hasPrevious());

            log.info("搜索完成，找到 {} 条记录", page.getTotalElements());
            return response;

        } catch (Exception e) {
            log.error("搜索标注数据失败", e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> filterDoubleTags(AnnotationFilterRequest request) {
        log.info("执行双标签筛选，请求参数: {}", request);

        if (request.getRequiredTags() == null || request.getRequiredTags().size() < 2) {
            throw new IllegalArgumentException("双标签筛选至少需要2个标签");
        }

        try {
            // 使用自定义查询筛选双标签
            Specification<Annotation> spec = buildDoubleTagsSpecification(request);
            
            // 分页查询
            Pageable pageable = buildPageable(request);
            Page<Annotation> page = annotationRepository.findAll(spec, pageable);

            // 转换结果
            List<Map<String, Object>> results = page.getContent().stream()
                    .map(this::convertAnnotationToMap)
                    .collect(Collectors.toList());

            // 统计标签组合
            Map<String, Integer> tagCombinations = analyzeTagCombinations(page.getContent(), request.getRequiredTags());

            Map<String, Object> response = new HashMap<>();
            response.put("content", results);
            response.put("totalElements", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("currentPage", page.getNumber());
            response.put("pageSize", page.getSize());
            response.put("tagCombinations", tagCombinations);
            response.put("searchTags", request.getRequiredTags());

            log.info("双标签筛选完成，找到 {} 条记录", page.getTotalElements());
            return response;

        } catch (Exception e) {
            log.error("双标签筛选失败", e);
            throw new RuntimeException("筛选失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Resource exportImages(AnnotationFilterRequest request) {
        log.info("开始导出图像文件，请求参数: {}", request);

        try {
            // 设置不分页，获取所有匹配的记录
            request.setPageSize(Integer.MAX_VALUE);
            request.setPageNumber(0);

            // 获取筛选结果
            Specification<Annotation> spec = request.getRequiredTags() != null && 
                request.getRequiredTags().size() >= 2 ? 
                buildDoubleTagsSpecification(request) : buildSpecification(request);

            List<Annotation> annotations = annotationRepository.findAll(spec);

            if (annotations.isEmpty()) {
                throw new RuntimeException("没有找到符合条件的记录");
            }

            // 创建临时目录
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path tempDir = Files.createTempDirectory("export_images_" + timestamp);
            Path zipFile = tempDir.resolve("filtered_images_" + timestamp + ".zip");

            log.info("创建临时目录: {}", tempDir);
            log.info("准备导出 {} 条记录", annotations.size());

            // 创建ZIP文件
            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                
                // 添加汇总文件
                addSummaryToZip(zipOut, annotations, request);

                int copiedCount = 0;
                int errorCount = 0;

                for (Annotation annotation : annotations) {
                    try {
                        if (copyImageToZip(zipOut, annotation)) {
                            copiedCount++;
                        } else {
                            errorCount++;
                        }
                    } catch (Exception e) {
                        log.warn("复制图像文件失败，标注ID: {}, 错误: {}", annotation.getId(), e.getMessage());
                        errorCount++;
                    }
                }

                log.info("图像文件导出完成，成功: {}, 失败: {}", copiedCount, errorCount);
            }

            // 返回ZIP文件资源
            return new FileSystemResource(zipFile);

        } catch (Exception e) {
            log.error("导出图像文件失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> getAnnotationUsers() {
        log.info("获取有标注记录的用户列表");

        try {
            // 使用原生查询获取用户统计信息
            List<User> allUsers = userRepository.findAll();
            
            return allUsers.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                
                // 统计该用户的标注数量
                long totalCount = annotationRepository.countByUserId(user.getId());
                long completedCount = annotationRepository.countByUserIdAndStatus(
                    user.getId(), AnnotationStatus.COMPLETED);
                
                userMap.put("annotationCount", totalCount);
                userMap.put("completedCount", completedCount);
                
                return userMap;
            })
            .filter(userMap -> (Long) userMap.get("annotationCount") > 0) // 只返回有标注的用户
            .sorted((a, b) -> Long.compare((Long) b.get("annotationCount"), (Long) a.get("annotationCount")))
            .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            throw new RuntimeException("获取用户列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getCommonTags() {
        log.info("获取常见标签列表");

        try {
            // 获取已完成的标注记录，用于分析常见标签
            Specification<Annotation> spec = (root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("status"), AnnotationStatus.COMPLETED);
            
            Pageable pageable = PageRequest.of(0, 1000);
            List<Annotation> completedAnnotations = annotationRepository.findAll(spec, pageable).getContent();

            Map<String, Integer> tagCounts = new HashMap<>();

            for (Annotation annotation : completedAnnotations) {
                Set<String> tags = extractTagsFromLabels(annotation.getLabels());
                for (String tag : tags) {
                    tagCounts.merge(tag, 1, Integer::sum);
                }
            }

            // 按频次排序，返回前50个最常用的标签
            return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(50)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取常见标签失败", e);
            throw new RuntimeException("获取常见标签失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> previewFilterResults(AnnotationFilterRequest request) {
        log.info("预览筛选结果，请求参数: {}", request);

        try {
            // 限制预览数量
            request.setPageSize(Math.min(request.getPageSize() != null ? request.getPageSize() : 10, 20));
            request.setPageNumber(0);

            Map<String, Object> searchResult = request.getRequiredTags() != null && 
                request.getRequiredTags().size() >= 2 ? 
                filterDoubleTags(request) : searchAnnotations(request);

            Map<String, Object> preview = new HashMap<>();
            preview.put("previewData", searchResult.get("content"));
            preview.put("totalCount", searchResult.get("totalElements"));
            preview.put("filterConditions", buildFilterSummary(request));

            return preview;

        } catch (Exception e) {
            log.error("预览筛选结果失败", e);
            throw new RuntimeException("预览失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getFilterStatistics(AnnotationFilterRequest request) {
        log.info("获取筛选统计，请求参数: {}", request);

        try {
            Specification<Annotation> spec = buildSpecification(request);
            long totalCount = annotationRepository.count(spec);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMatched", totalCount);
            stats.put("filterConditions", buildFilterSummary(request));

            return stats;

        } catch (Exception e) {
            log.error("获取筛选统计失败", e);
            throw new RuntimeException("获取统计失败: " + e.getMessage(), e);
        }
    }

    // 私有辅助方法

    private Specification<Annotation> buildSpecification(AnnotationFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 用户ID筛选
            if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(request.getUserIds()));
            }

            // 用户名筛选
            if (request.getUsernames() != null && !request.getUsernames().isEmpty()) {
                Join<Annotation, User> userJoin = root.join("user");
                predicates.add(userJoin.get("username").in(request.getUsernames()));
            }

            // 状态筛选
            if (request.getAnnotationStatus() != null && !request.getAnnotationStatus().trim().isEmpty()) {
                try {
                    AnnotationStatus status = AnnotationStatus.valueOf(request.getAnnotationStatus());
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的标注状态: {}", request.getAnnotationStatus());
                }
            }

            // 日期范围筛选
            if (request.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), request.getStartDate().atStartOfDay()));
            }
            if (request.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), request.getEndDate().atTime(23, 59, 59)));
            }

            // 图像ID筛选
            if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
                predicates.add(root.get("imageId").in(request.getImageIds()));
            }

            // 必需标签筛选（包含所有或任意标签）
            if (request.getRequiredTags() != null && !request.getRequiredTags().isEmpty()) {
                List<Predicate> tagPredicates = new ArrayList<>();

                for (String tag : request.getRequiredTags()) {
                    Expression<String> labelsExpr = root.get("labels").as(String.class);
                    // 直接对JSON字符串进行LIKE匹配，避免使用lower()函数造成类型冲突
                    tagPredicates.add(criteriaBuilder.like(
                        labelsExpr, 
                        "%" + tag + "%"));
                }

                if ("ALL".equals(request.getTagMatchMode()) || request.getRequiredTags().size() > 1) {
                    // 必须包含所有标签
                    predicates.addAll(tagPredicates);
                } else {
                    // 包含任意一个标签（单标签时默认使用）
                    predicates.add(criteriaBuilder.or(tagPredicates.toArray(new Predicate[0])));
                }
            }

            // 排除标签筛选
            if (request.getExcludeTags() != null && !request.getExcludeTags().isEmpty()) {
                for (String tag : request.getExcludeTags()) {
                    Expression<String> labelsExpr = root.get("labels").as(String.class);
                    // 直接对JSON字符串进行NOT LIKE匹配，避免使用lower()函数造成类型冲突
                    predicates.add(criteriaBuilder.notLike(
                        labelsExpr, 
                        "%" + tag + "%"));
                }
            }

            // 只返回有图像的记录
            if (Boolean.TRUE.equals(request.getHasImageOnly())) {
                Join<Annotation, Image> imageJoin = root.join("image");
                predicates.add(criteriaBuilder.isNotNull(imageJoin.get("id")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Annotation> buildDoubleTagsSpecification(AnnotationFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 先应用基本筛选条件
            Specification<Annotation> baseSpec = buildSpecification(request);
            predicates.add(baseSpec.toPredicate(root, query, criteriaBuilder));

            // 添加标签筛选条件
            if (request.getRequiredTags() != null && !request.getRequiredTags().isEmpty()) {
                List<Predicate> tagPredicates = new ArrayList<>();

                for (String tag : request.getRequiredTags()) {
                    Expression<String> labelsExpr = root.get("labels").as(String.class);
                    // 直接对JSON字符串进行LIKE匹配，避免使用lower()函数造成类型冲突
                    tagPredicates.add(criteriaBuilder.like(
                        labelsExpr, 
                        "%" + tag + "%"));
                }

                if ("ALL".equals(request.getTagMatchMode())) {
                    // 必须包含所有标签
                    predicates.addAll(tagPredicates);
                } else {
                    // 包含任意一个标签
                    predicates.add(criteriaBuilder.or(tagPredicates.toArray(new Predicate[0])));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(AnnotationFilterRequest request) {
        int page = request.getPageNumber() != null ? request.getPageNumber() : 0;
        int size = request.getPageSize() != null ? request.getPageSize() : 50;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private Map<String, Object> convertAnnotationToMap(Annotation annotation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", annotation.getId());
        map.put("imageId", annotation.getImageId());
        map.put("userId", annotation.getUserId());
        map.put("status", annotation.getStatus());
        map.put("createdAt", annotation.getCreatedAt());
        map.put("updatedAt", annotation.getUpdatedAt());

        // 解析并格式化标签
        if (annotation.getLabels() != null) {
            try {
                JsonNode labelsNode = objectMapper.readTree(annotation.getLabels());
                map.put("labels", labelsNode);
                map.put("labelsText", formatLabelsForDisplay(annotation.getLabels()));
            } catch (Exception e) {
                map.put("labels", annotation.getLabels());
                map.put("labelsText", annotation.getLabels());
            }
        }

        // 添加用户信息
        if (annotation.getUser() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", annotation.getUser().getId());
            userInfo.put("username", annotation.getUser().getUsername());
            map.put("user", userInfo);
        }

        // 添加图像信息
        if (annotation.getImage() != null) {
            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("id", annotation.getImage().getId());
            imageInfo.put("filename", annotation.getImage().getFilename());
            imageInfo.put("originalName", annotation.getImage().getOriginalName());
            imageInfo.put("filePath", annotation.getImage().getFilePath());
            map.put("image", imageInfo);
        }

        return map;
    }

    private Set<String> extractTagsFromLabels(String labelsJson) {
        Set<String> tags = new HashSet<>();
        try {
            JsonNode labelsNode = objectMapper.readTree(labelsJson);
            extractTagsRecursively(labelsNode, tags);
        } catch (Exception e) {
            log.warn("解析标签JSON失败: {}", labelsJson);
        }
        return tags;
    }

    private void extractTagsRecursively(JsonNode node, Set<String> tags) {
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    tags.add(item.asText());
                } else {
                    extractTagsRecursively(item, tags);
                }
            }
        } else if (node.isObject()) {
            node.fieldNames().forEachRemaining(fieldName -> {
                tags.add(fieldName);
                extractTagsRecursively(node.get(fieldName), tags);
            });
        } else if (node.isTextual()) {
            tags.add(node.asText());
        }
    }

    private String formatLabelsForDisplay(String labelsJson) {
        try {
            JsonNode labelsNode = objectMapper.readTree(labelsJson);
            Set<String> tags = new HashSet<>();
            extractTagsRecursively(labelsNode, tags);
            return String.join(", ", tags);
        } catch (Exception e) {
            return labelsJson;
        }
    }

    private Map<String, Integer> analyzeTagCombinations(List<Annotation> annotations, List<String> searchTags) {
        Map<String, Integer> combinations = new HashMap<>();
        
        for (Annotation annotation : annotations) {
            Set<String> tags = extractTagsFromLabels(annotation.getLabels());
            List<String> foundTags = searchTags.stream()
                .filter(tags::contains)
                .sorted()
                .collect(Collectors.toList());
            
            if (foundTags.size() >= 2) {
                String combination = String.join(" + ", foundTags);
                combinations.merge(combination, 1, Integer::sum);
            }
        }
        
        return combinations;
    }

    private void addSummaryToZip(ZipOutputStream zipOut, List<Annotation> annotations, 
                                 AnnotationFilterRequest request) throws IOException {
        ZipEntry summaryEntry = new ZipEntry("export_summary.txt");
        zipOut.putNextEntry(summaryEntry);

        StringBuilder summary = new StringBuilder();
        summary.append("图像导出汇总报告\n");
        summary.append("==========================================\n");
        summary.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        summary.append("导出记录数: ").append(annotations.size()).append("\n");
        
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            summary.append("筛选用户ID: ").append(request.getUserIds()).append("\n");
        }
        if (request.getRequiredTags() != null && !request.getRequiredTags().isEmpty()) {
            summary.append("筛选标签: ").append(String.join(", ", request.getRequiredTags())).append("\n");
        }
        
        summary.append("\n详细记录:\n");
        summary.append("------------------------------------------\n");
        
        int index = 1;
        for (Annotation annotation : annotations) {
            summary.append(String.format("%d. 标注ID: %d, 图像ID: %d\n", 
                index++, annotation.getId(), annotation.getImageId()));
            
            if (annotation.getImage() != null) {
                summary.append(String.format("   文件名: %s\n", annotation.getImage().getFilename()));
            }
            
            if (annotation.getUser() != null) {
                summary.append(String.format("   标注用户: %s\n", annotation.getUser().getUsername()));
            }
            
            summary.append(String.format("   创建时间: %s\n", annotation.getCreatedAt()));
            summary.append(String.format("   标签: %s\n\n", formatLabelsForDisplay(annotation.getLabels())));
        }

        zipOut.write(summary.toString().getBytes("UTF-8"));
        zipOut.closeEntry();
    }

    private boolean copyImageToZip(ZipOutputStream zipOut, Annotation annotation) throws IOException {
        if (annotation.getImage() == null) {
            return false;
        }

        Image image = annotation.getImage();
        Path imagePath = null;

        // 尝试多个可能的路径
        String[] possiblePaths = {
            image.getFilePath(),
            uploadPath + "/" + image.getFilename(),
            "./uploads/" + image.getFilename(),
            "uploads/" + image.getFilename()
        };

        for (String pathStr : possiblePaths) {
            if (pathStr != null) {
                Path path = Paths.get(pathStr);
                if (Files.exists(path)) {
                    imagePath = path;
                    break;
                }
            }
        }

        if (imagePath == null || !Files.exists(imagePath)) {
            log.warn("图像文件不存在: {}", image.getFilename());
            return false;
        }

        // 添加到ZIP文件
        String entryName = String.format("%d_%s", annotation.getImageId(), image.getFilename());
        ZipEntry entry = new ZipEntry(entryName);
        zipOut.putNextEntry(entry);
        Files.copy(imagePath, zipOut);
        zipOut.closeEntry();

        return true;
    }

    private Map<String, Object> buildFilterSummary(AnnotationFilterRequest request) {
        Map<String, Object> summary = new HashMap<>();
        
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            summary.put("userIds", request.getUserIds());
        }
        if (request.getUsernames() != null && !request.getUsernames().isEmpty()) {
            summary.put("usernames", request.getUsernames());
        }
        if (request.getRequiredTags() != null && !request.getRequiredTags().isEmpty()) {
            summary.put("requiredTags", request.getRequiredTags());
        }
        if (request.getAnnotationStatus() != null) {
            summary.put("status", request.getAnnotationStatus());
        }
        if (request.getStartDate() != null) {
            summary.put("startDate", request.getStartDate());
        }
        if (request.getEndDate() != null) {
            summary.put("endDate", request.getEndDate());
        }
        
        return summary;
    }
}
