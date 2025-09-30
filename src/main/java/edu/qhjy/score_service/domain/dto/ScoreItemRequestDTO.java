package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成绩分项请求DTO
 */
@Data
@Schema(description = "成绩分项请求")
public class ScoreItemRequestDTO {

    @Schema(description = "分项ID（更新时使用）")
    private Long kscjfxbs;

    @NotNull(message = "序号不能为空")
    @Min(value = 1, message = "序号必须大于0")
    @Schema(description = "序号", example = "1")
    private Integer fxxh;

    @NotBlank(message = "分项名称不能为空")
    @Schema(description = "分项名称", example = "过程性评价")
    private String fxmc;

    @NotNull(message = "分项成绩分数不能为空")
    @Min(value = 1, message = "分项成绩分数必须大于0")
    @Max(value = 100, message = "分项成绩分数不能超过100")
    @Schema(description = "分项成绩分数", example = "40")
    private Integer fxcjfs;
}