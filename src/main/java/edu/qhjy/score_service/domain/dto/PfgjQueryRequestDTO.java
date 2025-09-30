package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 评分轨迹查询请求DTO
 *
 * @author dadalv
 * @since 2025-08-20
 */
@Data
@Schema(description = "评分轨迹查询请求")
public class PfgjQueryRequestDTO {

    @Schema(description = "考试计划代码", example = "202501")
    @NotBlank(message = "考试计划代码不能为空")
    private String ksjhdm;

    @Schema(description = "考生号", example = "20250101001")
    @NotBlank(message = "考生号不能为空")
    private String ksh;

    @Schema(description = "科目名称", example = "语文")
    @NotBlank(message = "科目名称不能为空")
    private String kmmc;
}