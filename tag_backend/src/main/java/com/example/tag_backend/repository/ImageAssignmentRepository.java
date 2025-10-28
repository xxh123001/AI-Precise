package com.example.tag_backend.repository;

import com.example.tag_backend.entity.ImageAssignment;
import com.example.tag_backend.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 图像分配仓库接口
 * 提供图像分配数据的访问方法
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Repository
public interface ImageAssignmentRepository extends JpaRepository<ImageAssignment, Long>, JpaSpecificationExecutor<ImageAssignment> {

    /**
     * 根据用户ID查询分配的任务
     */
    Page<ImageAssignment> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID查询分配的任务（带关联加载）
     */
    @Query("SELECT ia FROM ImageAssignment ia " +
           "LEFT JOIN FETCH ia.image i " +
           "LEFT JOIN FETCH ia.assigner a " +
           "WHERE ia.userId = :userId " +
           "ORDER BY ia.assignedAt DESC")
    List<ImageAssignment> findByUserIdWithAssociations(@Param("userId") Long userId);


    /**
     * 根据图像ID查询分配记录
     */
    List<ImageAssignment> findByImageId(Long imageId);

    /**
     * 根据用户ID和状态查询
     */
    Page<ImageAssignment> findByUserIdAndStatus(Long userId, AssignmentStatus status, Pageable pageable);

    /**
     * 查找指定图像和用户的分配记录
     */
    Optional<ImageAssignment> findByImageIdAndUserId(Long imageId, Long userId);

    /**
     * 检查图像是否已分配给指定用户
     */
    boolean existsByImageIdAndUserId(Long imageId, Long userId);

    /**
     * 统计用户的任务数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户指定状态的任务数量
     */
    long countByUserIdAndStatus(Long userId, AssignmentStatus status);

    /**
     * 根据分配者查询分配记录
     */
    Page<ImageAssignment> findByAssignedBy(Long assignedBy, Pageable pageable);

    /**
     * 统计指定状态的分配数量
     */
    long countByStatus(AssignmentStatus status);
    
    /**
     * 根据用户ID查询所有任务，按分配时间倒序排列
     */
    @Query("SELECT ia FROM ImageAssignment ia " +
           "LEFT JOIN FETCH ia.image i " +
           "WHERE ia.userId = :userId " +
           "ORDER BY ia.assignedAt DESC")
    List<ImageAssignment> findByUserIdOrderByAssignedAtDesc(@Param("userId") Long userId);
    
    /**
     * 根据任务ID和用户ID查询任务（用于权限验证）
     */
    @Query("SELECT ia FROM ImageAssignment ia " +
           "LEFT JOIN FETCH ia.image i " +
           "LEFT JOIN FETCH i.labelConfig lc " +
           "LEFT JOIN FETCH ia.assigner a " +
           "WHERE ia.id = :id AND ia.userId = :userId")
    Optional<ImageAssignment> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据项目ID查询所有任务
     */
    @Query("SELECT ia FROM ImageAssignment ia " +
           "LEFT JOIN FETCH ia.image i " +
           "LEFT JOIN FETCH ia.user u " +
           "WHERE ia.projectId = :projectId " +
           "ORDER BY ia.assignedAt DESC")
    List<ImageAssignment> findByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据项目ID和用户ID查询任务
     */
    @Query("SELECT ia FROM ImageAssignment ia " +
           "LEFT JOIN FETCH ia.image i " +
           "WHERE ia.projectId = :projectId AND ia.userId = :userId " +
           "ORDER BY ia.assignedAt DESC")
    List<ImageAssignment> findByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 统计项目的任务数量
     */
    long countByProjectId(Long projectId);

    /**
     * 统计项目指定状态的任务数量
     */
    long countByProjectIdAndStatus(Long projectId, AssignmentStatus status);

    /**
     * 统计用户在指定项目中的任务数量
     */
    long countByUserIdAndProjectId(Long userId, Long projectId);

    /**
     * 统计用户在指定项目中指定状态的任务数量
     */
    long countByUserIdAndProjectIdAndStatus(Long userId, Long projectId, AssignmentStatus status);
}
