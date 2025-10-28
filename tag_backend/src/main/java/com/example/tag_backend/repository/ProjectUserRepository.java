package com.example.tag_backend.repository;

import com.example.tag_backend.entity.ProjectUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目用户关联数据访问接口
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-16
 */
@Repository
public interface ProjectUserRepository extends JpaRepository<ProjectUser, Long> {

    /**
     * 查找项目的所有用户
     */
    List<ProjectUser> findByProjectId(Long projectId);

    /**
     * 查找用户参与的所有项目
     */
    List<ProjectUser> findByUserId(Long userId);

    /**
     * 查找特定的项目-用户关联
     */
    Optional<ProjectUser> findByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 检查用户是否在项目中
     */
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 删除项目的所有用户关联
     */
    void deleteByProjectId(Long projectId);

    /**
     * 删除特定的项目-用户关联
     */
    void deleteByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * 统计项目的用户数量
     */
    long countByProjectId(Long projectId);

    /**
     * 查找项目的活跃用户
     */
    @Query("SELECT pu FROM ProjectUser pu WHERE pu.projectId = :projectId AND pu.status = 'active'")
    List<ProjectUser> findActiveUsersByProjectId(@Param("projectId") Long projectId);
}


