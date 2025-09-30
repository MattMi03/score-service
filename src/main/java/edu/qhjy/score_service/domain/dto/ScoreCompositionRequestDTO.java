package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 成绩构成保存请求DTO
 */
@Data
@Schema(description = "成绩构成保存请求")
public class ScoreCompositionRequestDTO {

    @NotBlank(message = "科目名称不能为空")
    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Valid
    @NotEmpty(message = "分项配置不能为空")
    @Size(max = 10, message = "分项数量不能超过10个")
    @Schema(description = "分项列表")
    private List<ScoreItemRequestDTO> items;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作人工作人员码")
    private String operatorCode;
}