package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成绩等第册查询请求DTO
 * 支持级联查询和分页
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩等第册查询请求")
public class GradeBookQueryDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码", example = "202507001")
    private String ksjhdm;

    @NotBlank(message = "学校不能为空")
    @Schema(description = "学校", example = "长沙市第一中学")
    private String school;

    @Schema(description = "所在省名称", example = "湖南省")
    private String szsmc;

    @Schema(description = "考区名称", example = "岳麓区")
    private String kqmc;

    @Schema(description = "分页页码（从1开始）")
    @Builder.Default
    private Integer pageNum = 1;

    @Schema(description = "分页大小（默认20）")
    @Builder.Default
    private Integer pageSize = 20;

    /**
     * 验证查询参数
     */
    public boolean isValid() {
        if (pageNum == null || pageNum < 1) {
            return false;
        }
        return pageSize != null && pageSize >= 1;
    }

    /**
     * 获取有效的页码
     */
    public Integer getValidPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    /**
     * 获取有效的页大小
     */
    public Integer getValidPageSize() {
        if (pageSize == null || pageSize <= 0) {
            return 20;
        }
        return pageSize;
    }

    /**
     * 获取查询偏移量
     */
    public Integer getOffset() {
        return (getValidPageNum() - 1) * getValidPageSize();
    }

    /**
     * 获取查询限制数量
     */
    public Integer getLimit() {
        return getValidPageSize();
    }

    /**
     * 验证参数并设置默认值
     */
    public void validateAndSetDefaults() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        // 移除分页大小限制
    }
}