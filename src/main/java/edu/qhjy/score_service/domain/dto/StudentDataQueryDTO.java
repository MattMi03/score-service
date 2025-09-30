package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生数据查询请求DTO
 * 支持多维度筛选和分页查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生数据查询请求")
public class StudentDataQueryDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码", example = "202507001")
    private String ksjhdm;

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "地市名称", example = "长沙市")
    private String szsmc;

    @Schema(description = "考区名称（区县）", example = "岳麓区")
    private String kqmc;

    @Schema(description = "学校名称", example = "长沙市第一中学")
    private String xxmc;

    @Schema(description = "考籍号", example = "202501001")
    private String ksh;

    @Schema(description = "考生姓名", example = "张三")
    private String ksxm;

    @Schema(description = "班级名称", example = "高三1班")
    private String bjmc;

    @Schema(description = "分页页码（从1开始）")
    @Builder.Default
    private Integer pageNum = 1;

    @Schema(description = "分页大小（默认20）")
    @Builder.Default
    private Integer pageSize = 20;

    @Schema(description = "是否只查询有成绩的学生")
    @Builder.Default
    private Boolean onlyWithScore = false;

    @Schema(description = "成绩范围过滤 - 最低分")
    private Integer minScore;

    @Schema(description = "成绩范围过滤 - 最高分")
    private Integer maxScore;

    @Schema(description = "合格状态过滤（合格/不合格）")
    private String cjhgm;

    @Schema(description = "等第过滤（A/B/C/D/E）")
    private String cjdjm;

    @Schema(description = "排序字段（ksh-考籍号, kscjbs-考生成绩标识, fslkscj-分数）")
    @Builder.Default
    private String sortField = "ksh";

    @Schema(description = "排序方向（asc-升序, desc-降序）")
    @Builder.Default
    private String sortOrder = "asc";

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
        return minScore == null || maxScore == null || minScore <= maxScore;
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
        if (onlyWithScore == null) {
            onlyWithScore = false;
        }
    }
}