package com.example.tag_backend.repository;

import com.example.tag_backend.entity.Annotation;
import com.example.tag_backend.enums.AnnotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 标注仓库接口
 * 提供标注数据的访问方法
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, Long>, JpaSpecificationExecutor<Annotation> {

    /**
     * 根据用户ID查询标注记录
     */
    Page<Annotation> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据图像ID查询标注记录
     */
    List<Annotation> findByImageId(Long imageId);

    /**
     * 查找指定图像和用户的标注记录
     */
    Optional<Annotation> findByImageIdAndUserId(Long imageId, Long userId);

    /**
     * 根据用户ID和状态查询
     */
    Page<Annotation> findByUserIdAndStatus(Long userId, AnnotationStatus status, Pageable pageable);

    /**
     * 检查图像是否已被用户标注
     */
    boolean existsByImageIdAndUserId(Long imageId, Long userId);

    /**
     * 统计用户的标注数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户指定状态的标注数量
     */
    long countByUserIdAndStatus(Long userId, AnnotationStatus status);

    /**
     * 统计指定时间范围内的标注数量
     */
    @Query("SELECT COUNT(a) FROM Annotation a WHERE a.createdAt BETWEEN :startTime AND :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询需要导出的标注数据
     */
    @Query("SELECT a FROM Annotation a " +
           "WHERE (:userId IS NULL OR a.userId = :userId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:startTime IS NULL OR a.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR a.createdAt <= :endTime)")
    List<Annotation> findForExport(@Param("userId") Long userId,
                                  @Param("status") AnnotationStatus status,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定状态的标注数量
     */
    long countByStatus(AnnotationStatus status);

    /**
     * 统计指定状态且在指定时间之后更新的标注数量
     */
    long countByStatusAndUpdatedAtAfter(AnnotationStatus status, LocalDateTime updatedAt);

    /**
     * 根据状态查询标注记录
     */
    List<Annotation> findByStatus(AnnotationStatus status);

}
