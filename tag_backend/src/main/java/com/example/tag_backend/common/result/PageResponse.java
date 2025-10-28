package com.example.tag_backend.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应类
 * 提供标准化的分页数据格式
 * 
 * @author Felix
 * @version 1.0
 * @since 2023-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 当前页数据
     */
    private List<T> content;

    /**
     * 分页信息
     */
    private Pagination pagination;

    /**
     * 分页信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        /**
         * 当前页码
         */
        private Integer page;

        /**
         * 每页大小
         */
        private Integer size;

        /**
         * 总记录数
         */
        private Long totalElements;

        /**
         * 总页数
         */
        private Integer totalPages;

        /**
         * 是否有下一页
         */
        private Boolean hasNext;

        /**
         * 是否有上一页
         */
        private Boolean hasPrevious;
    }

    /**
     * 从Spring Data Page对象创建PageResponse
     */
    public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
        Pagination pagination = new Pagination(
            page.getNumber() + 1, // Spring Data Page是从0开始的，转换为从1开始
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
        
        return new PageResponse<>(content, pagination);
    }
}
