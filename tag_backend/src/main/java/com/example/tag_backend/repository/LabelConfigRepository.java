package com.example.tag_backend.repository;

import com.example.tag_backend.entity.LabelConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标签配置仓库接口
 * 提供标签配置数据的访问方法
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Repository
public interface LabelConfigRepository extends JpaRepository<LabelConfig, Long>, JpaSpecificationExecutor<LabelConfig> {

    /**
     * 查找所有激活的标签配置
     */
    List<LabelConfig> findByIsActiveTrue();

    /**
     * 根据激活状态分页查询
     */
    Page<LabelConfig> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * 根据创建者查询
     */
    Page<LabelConfig> findByCreatedBy(Long createdBy, Pageable pageable);

    /**
     * 根据名称模糊查询
     */
    Page<LabelConfig> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 统计激活的标签配置数量
     */
    long countByIsActiveTrue();
}
