package com.example.tag_backend.repository;

import com.example.tag_backend.entity.User;
import com.example.tag_backend.enums.UserStatus;
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
 * 用户仓库接口
 * 提供用户数据的访问方法
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据用户名和状态查找用户
     */
    Optional<User> findByUsernameAndStatus(String username, UserStatus status);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据状态分页查询用户
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * 根据用户名模糊查询
     */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /**
     * 根据用户名和状态模糊查询
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> findByUsernameAndStatus(@Param("username") String username, 
                                      @Param("status") UserStatus status, 
                                      Pageable pageable);

    /**
     * 统计活跃用户数量
     */
    long countByStatus(UserStatus status);

    /**
     * 统计指定时间之后创建的用户数量
     */
    long countByCreatedAtAfter(java.time.LocalDateTime createdAt);

    /**
     * 查询有标注记录的用户列表
     */
    @Query("SELECT DISTINCT u FROM User u WHERE u.id IN " +
           "(SELECT DISTINCT a.userId FROM Annotation a)")
    List<User> findUsersWithAnnotations();
}
