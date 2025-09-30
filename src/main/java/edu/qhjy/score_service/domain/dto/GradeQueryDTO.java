package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成绩查询请求DTO
 * 支持级联查询和分页查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩查询请求")
public class GradeQueryDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码", example = "202507001")
    private String ksjhdm;

    @Schema(description = "地市名称", example = "长沙市")
    private String szsmc;

    @Schema(description = "考区名称（区县）", example = "岳麓区")
    private String kqmc;

    @Schema(description = "学校名称", example = "长沙市第一中学")
    private String xxmc;

    @Schema(description = "年级（入学年度）", example = "2023")
    private String grade;

    @Schema(description = "班级名称", example = "高三1班")
    private String bjmc;

    @Schema(description = "考籍号", example = "202501001")
    private String ksh;

    @Schema(description = "科目类型（0:合格性考试科目，1:考察性考试科目）", example = "0")
    @Builder.Default
    private Integer kmlx = 0;

    @Schema(description = "分页页码（从1开始）")
    @Builder.Default
    private Integer pageNum = 1;

    @Schema(description = "分页大小（默认20）")
    @Builder.Default
    private Integer pageSize = 20;

    @Schema(description = "排序字段（ksh-考籍号, xm-姓名, fslkscj-分数）")
    @Builder.Default
    private String sortField = "ksh";

    @Schema(description = "排序方向（asc-升序, desc-降序）")
    @Builder.Default
    private String sortOrder = "asc";

    @Schema(description = "是否只返回有成绩的考生（true-仅返回KKLXMC='正考'，false-返回所有考生包括'正考'和'缺考'）")
    @Builder.Default
    private Boolean onlyWithScores = false;

    private String permissionDm;
    /**
     * 验证查询参数
     */
    public boolean isValid() {
        if (pageNum == null || pageNum < 1) {
            return false;
        }
        if (pageSize == null || pageSize < 1) {
            return false;
        }
        return kmlx == null || (kmlx >= 0 && kmlx <= 1);
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
        return pageSize;
    }

    /**
     * 获取偏移量
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
        // 移除分页大小限制
        if (kmlx == null) {
            kmlx = 0;
        }
        if (sortField == null || sortField.trim().isEmpty()) {
            sortField = "ksh";
        }
        if (sortOrder == null || sortOrder.trim().isEmpty()) {
            sortOrder = "asc";
        }
    }
}