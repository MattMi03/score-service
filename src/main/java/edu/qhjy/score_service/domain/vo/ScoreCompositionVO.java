package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成绩构成VO
 * 用于前端展示科目的分项构成信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩构成信息")
public class ScoreCompositionVO {

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "分项列表")
    private List<ScoreItemVO> items;

    @Schema(description = "总分", example = "100")
    private Integer totalScore;

    @Schema(description = "是否已被使用（有学生分项成绩数据）")
    private Boolean inUse;

    @Schema(description = "使用该配置的考试计划数量")
    private Integer usedPlanCount;

    @Schema(description = "配置是否完整（总分是否为100）")
    private Boolean isComplete;
}