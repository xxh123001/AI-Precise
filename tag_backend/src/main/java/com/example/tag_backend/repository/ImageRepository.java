package com.example.tag_backend.repository;

import com.example.tag_backend.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 图像仓库接口
 * 提供图像数据的访问方法
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long>, JpaSpecificationExecutor<Image> {

    /**
     * 根据上传者查询图像
     */
    Page<Image> findByUploadBy(Long uploadBy, Pageable pageable);

    /**
     * 根据标签配置查询图像
     */
    Page<Image> findByLabelConfigId(Long labelConfigId, Pageable pageable);

    /**
     * 根据文件名查找图像
     */
    Image findByFilename(String filename);

    /**
     * 根据原始文件名模糊查询
     */
    Page<Image> findByOriginalNameContainingIgnoreCase(String originalName, Pageable pageable);

    /**
     * 查询指定用户分配的图像列表
     */
    @Query("SELECT i FROM Image i " +
           "JOIN ImageAssignment ia ON i.id = ia.imageId " +
           "WHERE ia.userId = :userId")
    Page<Image> findAssignedImagesByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计图像总数
     */
    long count();

    /**
     * 统计指定用户上传的图像数量
     */
    long countByUploadBy(Long uploadBy);

    /**
     * 统计使用指定标签配置的图像数量
     */
    long countByLabelConfigId(Long labelConfigId);

    /**
     * 统计指定时间之后上传的图像数量
     */
    long countByCreatedAtAfter(java.time.LocalDateTime createdAt);
    
    /**
     * 查询已分配的图像
     */
    @Query("SELECT DISTINCT i FROM Image i " +
           "JOIN ImageAssignment ia ON i.id = ia.imageId")
    Page<Image> findAssignedImages(Pageable pageable);
    
    /**
     * 查询未分配的图像
     */
    @Query("SELECT i FROM Image i " +
           "WHERE i.id NOT IN (SELECT DISTINCT ia.imageId FROM ImageAssignment ia)")
    Page<Image> findUnassignedImages(Pageable pageable);
}
