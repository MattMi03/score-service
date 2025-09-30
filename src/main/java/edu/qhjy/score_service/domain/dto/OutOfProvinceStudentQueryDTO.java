package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 省外转入考生查询DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "省外转入考生查询请求参数")
public class OutOfProvinceStudentQueryDTO {

    /**
     * 省市名称（非必填）
     */
    @Schema(description = "省市名称", example = "长沙市")
    private String szsmc;

    /**
     * 考区名称（非必填）
     */
    @Schema(description = "考区名称", example = "岳麓区")
    private String kqmc;

    /**
     * 学校名称（非必填）
     */
    @Schema(description = "学校名称", example = "长沙市第一中学")
    private String xxmc;

    /**
     * 考生号（非必填）
     */
    @Schema(description = "考生号", example = "2024001001")
    private String ksh;

    /**
     * 页码（从1开始）
     */
    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20", defaultValue = "20")
    private Integer pageSize;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "CJSJ")
    private String sortField;

    /**
     * 排序方向（ASC/DESC）
     */
    @Schema(description = "排序方向", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String sortOrder;

    /**
     * 验证并设置默认值
     */
    public void validateAndSetDefaults() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100; // 限制最大页面大小
        }
        if (sortField == null || sortField.trim().isEmpty()) {
            sortField = "CJSJ";
        }
        if (sortOrder == null || (!"ASC".equalsIgnoreCase(sortOrder) && !"DESC".equalsIgnoreCase(sortOrder))) {
            sortOrder = "DESC";
        }
    }

    /**
     * 获取有效的页码
     */
    public Integer getValidPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    /**
     * 获取有效的分页大小
     */
    public Integer getValidPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    /**
     * 计算偏移量
     */
    public Integer getOffset() {
        return (getValidPageNum() - 1) * getValidPageSize();
    }

    /**
     * 获取限制数量
     */
    public Integer getLimit() {
        return getValidPageSize();
    }
}