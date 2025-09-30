package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 毕业生花名册PDF下载查询请求DTO
 * 用于生成毕业生花名册PDF文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "毕业生花名册PDF下载查询请求")
public class GraduationPdfQueryDTO {

    @NotBlank(message = "所在市名称不能为空")
    @Schema(description = "所在市名称", required = true, example = "西宁市")
    private String szsmc;

    @Schema(description = "考区名称（区县）", example = "城东区")
    private String kqmc;

    @Schema(description = "学校名称", example = "青海省第一中学")
    private String xxmc;

    @Schema(description = "毕业年度", example = "2024")
    private String bynd;

    @Schema(description = "考生号", example = "202401001")
    private String ksh;

    @Schema(description = "排序字段（ksh-考生号, xm-姓名, bynd-毕业年度）")
    @Builder.Default
    private String sortField = "ksh";

    @Schema(description = "排序方向（asc-升序，desc-降序）")
    @Builder.Default
    private String sortOrder = "asc";

    @Schema(description = "是否满足毕业条件（筛选条件）：true-只返回满足毕业条件的学生，false或null-返回所有学生")
    private Boolean isQualified;
}