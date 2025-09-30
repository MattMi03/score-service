package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 毕业生条件查询请求DTO
 * 支持级联查询和分页查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "毕业生条件查询请求")
public class GraduationQueryDTO {

    @NotBlank(message = "所在市名称不能为空")
    @Schema(description = "所在市名称", example = "西宁市")
    private String szsmc;

    @Schema(description = "考区名称（区县）", example = "城东区")
    private String kqmc;

    @Schema(description = "学校名称", example = "青海省第一中学")
    private String xxmc;

    @Schema(description = "毕业年度", example = "2024")
    private String bynd;

    @Schema(description = "考生号", example = "202401001")
    private String ksh;

    @Schema(description = "分页页码（从1开始）")
    @Builder.Default
    private Integer pageNum = 1;

    @Schema(description = "分页大小（默认20）")
    @Builder.Default
    private Integer pageSize = 20;

    @Schema(description = "排序字段（ksh-考生号, xm-姓名, bynd-毕业年度）")
    @Builder.Default
    private String sortField = "ksh";

    @Schema(description = "排序方向（asc-升序，desc-降序）")
    @Builder.Default
    private String sortOrder = "asc";

    @Schema(description = "是否满足毕业条件（筛选条件）：true-只返回满足毕业条件的学生，false或null-返回所有学生")
    private Boolean isQualified;

    /**
     * 计算分页偏移量
     *
     * @return 偏移量
     */
    public Integer getOffset() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        return (pageNum - 1) * pageSize;
    }
}