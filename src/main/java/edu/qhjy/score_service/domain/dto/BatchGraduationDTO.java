package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量毕业审批请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量毕业审批请求")
public class BatchGraduationDTO {

    @NotBlank(message = "所在市名称不能为空")
    @Schema(description = "所在市名称", example = "西宁市")
    private String szsmc;

    @Schema(description = "考区名称", example = "城东区")
    private String kqmc;

    @Schema(description = "学校名称", example = "青海省第一中学")
    private String xxmc;

    @Schema(description = "考生号", example = "202401001")
    private String ksh;

    @Schema(description = "毕业年度（可选，不传则使用当前年份）", example = "2024")
    private String bynd;

    @Schema(description = "操作人姓名", example = "管理员")
    private String operatorName;

    @Schema(description = "操作人工作人员码（可选，长度不超过10个字符）", example = "ADMIN001")
    private String operatorCode;
}