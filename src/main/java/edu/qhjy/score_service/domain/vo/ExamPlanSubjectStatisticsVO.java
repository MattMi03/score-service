package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 考试计划科目统计信息VO
 */
@Data
@Schema(description = "考试计划科目统计信息")
public class ExamPlanSubjectStatisticsVO {

    @Schema(description = "考试计划代码", example = "202507")
    private String ksjhdm;

    @Schema(description = "考试计划名称", example = "2025年7月普通高中学业水平合格性考试")
    private String ksjhmc;

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "学生人数", example = "167")
    private Integer studentCount;
}