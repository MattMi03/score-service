package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 等级划分请求DTO
 */
@Data
@Schema(description = "等级划分请求参数")
public class GradeAssignmentRequestDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码", example = "202503")
    private String ksjhdm;

    @NotBlank(message = "科目名称不能为空")
    @Schema(description = "科目名称", example = "数学")
    private String kmmc;

    @Schema(description = "市州名称，为空时处理所有市州", example = "海东市")
    private String szsmc;

    @Schema(description = "操作人姓名", example = "管理员")
    private String operatorName;

    @Schema(description = "操作人工作人员码", example = "ADMIN001")
    private String operatorCode;
}