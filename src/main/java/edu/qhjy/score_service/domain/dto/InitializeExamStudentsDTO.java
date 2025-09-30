package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 导入考试科目考生（初始化）请求DTO
 */
@Data
@Schema(description = "导入考试科目考生（初始化）请求DTO")
public class InitializeExamStudentsDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码", example = "2024001")
    private String ksjhdm;

    @NotBlank(message = "科目名称不能为空")
    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @NotBlank(message = "所在市名称不能为空")
    @Schema(description = "所在市名称", example = "长沙市")
    private String szsmc;

    @Schema(description = "考区名称（非必填，若不填则初始化该市州所有考区）", example = "岳麓区")
    private String kqmc;

    @Schema(description = "学校名称（非必填，若不填则初始化该考区所有学校）", example = "湖南师范大学附属中学")
    private String xxmc;
}