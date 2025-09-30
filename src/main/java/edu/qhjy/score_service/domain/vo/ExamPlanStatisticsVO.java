package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考试计划统计信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考试计划统计信息")
public class ExamPlanStatisticsVO {

    @Schema(description = "考试计划代码", example = "202507")
    private String ksjhdm;

    @Schema(description = "考试计划名称", example = "2025年7月青海省学业水平合格性考试")
    private String ksjhmc;

    @Schema(description = "科目名称", example = "化学")
    private String kmmc;

    @Schema(description = "学生数量", example = "1")
    private Integer studentCount;
}