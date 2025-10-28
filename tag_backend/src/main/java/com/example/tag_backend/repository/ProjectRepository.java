package com.example.tag_backend.repository;

import com.example.tag_backend.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 项目数据访问接口
 * 
 * @author 辛晓红
 * @version 1.0
 * @since 2025-10-16
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 根据状态查找项目
     */
    List<Project> findByStatus(String status);

    /**
     * 根据创建者查找项目
     */
    List<Project> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    /**
     * 分页查询项目
     */
    Page<Project> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * 根据名称模糊查询
     */
    @Query("SELECT p FROM Project p WHERE p.name LIKE %:name% ORDER BY p.createdAt DESC")
    List<Project> findByNameContaining(@Param("name") String name);

    /**
     * 查询活跃项目
     */
    List<Project> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}
