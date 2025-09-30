package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一分一段数据变化预览VO
 */
@Data
@Schema(description = "一分一段数据变化预览结果")
public class ScoreSegmentChangePreviewVO {

    @Schema(description = "原始一分一段数据")
    private List<ScoreSegmentData> originalData;

    @Schema(description = "调整后一分一段数据")
    private List<ScoreSegmentData> adjustedData;

    @Schema(description = "变化统计信息")
    private ChangeStatistics changeStatistics;

    /**
     * 一分一段数据项
     */
    @Data
    @Schema(description = "一分一段数据项")
    public static class ScoreSegmentData {
        @Schema(description = "分数")
        private BigDecimal score;

        @Schema(description = "该分数人数")
        private Integer count;

        @Schema(description = "累计人数")
        private Integer cumulativeCount;

        @Schema(description = "累计百分比")
        private BigDecimal cumulativePercentage;

        @Schema(description = "等级")
        private String grade;
    }

    /**
     * 变化统计信息
     */
    @Data
    @Schema(description = "变化统计信息")
    public static class ChangeStatistics {
        @Schema(description = "总受影响分数段数量")
        private Integer totalAffectedScores;

        @Schema(description = "等级变化的分数段数量")
        private Integer gradeChangedScores;

        @Schema(description = "各等级分界线变化")
        private List<GradeThresholdChange> gradeThresholdChanges;
    }

    /**
     * 等级分界线变化
     */
    @Data
    @Schema(description = "等级分界线变化")
    public static class GradeThresholdChange {
        @Schema(description = "等级")
        private String grade;

        @Schema(description = "原始分界线")
        private BigDecimal originalThreshold;

        @Schema(description = "调整后分界线")
        private BigDecimal adjustedThreshold;

        @Schema(description = "分界线变化")
        private BigDecimal thresholdChange;
    }
}