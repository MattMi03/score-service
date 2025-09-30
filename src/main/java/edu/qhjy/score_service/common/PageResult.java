package edu.qhjy.score_service.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果封装类
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页码（从1开始）
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否为第一页
     */
    private Boolean isFirst;

    /**
     * 是否为最后一页
     */
    private Boolean isLast;

    /**
     * 构建分页结果
     *
     * @param records  当前页数据
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param total    总记录数
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Integer pageNum, Integer pageSize, Long total) {
        // 计算总页数
        int pages = (int) Math.ceil((double) total / pageSize);

        // 构建分页信息
        return PageResult.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .pages(pages)
                .records(records)
                .hasPrevious(pageNum > 1)
                .hasNext(pageNum < pages)
                .isFirst(pageNum == 1)
                .isLast(pageNum >= pages)
                .build();
    }

    /**
     * 构建空的分页结果
     *
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        return PageResult.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(0L)
                .pages(0)
                .records(List.of())
                .hasPrevious(false)
                .hasNext(false)
                .isFirst(true)
                .isLast(true)
                .build();
    }

    /**
     * 检查分页参数是否有效
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 是否有效
     */
    public static boolean isValidPageParams(Integer pageNum, Integer pageSize) {
        return pageNum != null && pageNum > 0 && pageSize != null && pageSize > 0;
    }

    /**
     * 获取默认页码
     *
     * @param pageNum 输入页码
     * @return 有效页码
     */
    public static Integer getValidPageNum(Integer pageNum) {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    /**
     * 获取默认页大小
     *
     * @param pageSize 输入页大小
     * @return 有效页大小
     */
    public static Integer getValidPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20; // 默认每页20条
        }
        return pageSize; // 移除分页大小限制
    }

    /**
     * 构建成功的分页结果
     *
     * @param records  当前页数据
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> success(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        return PageResult.of(records, pageNum, pageSize, total);
    }

    /**
     * 获取当前页起始索引（从0开始）
     *
     * @return 起始索引
     */
    public Integer getStartIndex() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 获取当前页结束索引（不包含）
     *
     * @return 结束索引
     */
    public Integer getEndIndex() {
        return Math.min(getStartIndex() + pageSize, total.intValue());
    }
}