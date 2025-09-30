package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 一分一段表总览视图对象
 * 用于前端展示一分一段表的总览数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "一分一段表总览数据")
public class ScoreSegmentOverviewVO {

    @Schema(description = "考试计划代码")
    private String ksjhdm;

    @Schema(description = "考试计划名称")
    private String ksjhmc;

    @Schema(description = "科目名称")
    private String kmmc;

    @Schema(description = "市州名称")
    private String szsmc;

    @Schema(description = "总人数")
    private Integer totalCount;

    @Schema(description = "最高分")
    private BigDecimal maxScore;

    @Schema(description = "最低分")
    private BigDecimal minScore;

    @Schema(description = "平均分")
    private BigDecimal avgScore;

    @Schema(description = "等级分布统计")
    private Map<String, GradeStatistics> gradeDistribution;

    @Schema(description = "市州分布统计")
    private List<CityStatistics> cityDistribution;

    @Schema(description = "一分一段详细数据")
    private List<ScoreSegmentData> segmentData;

    @Schema(description = "数据更新时间")
    private String updateTime;

    @Schema(description = "是否支持等级调整")
    private Boolean adjustable;

    /**
     * 等级统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "等级统计信息")
    public static class GradeStatistics {

        @Schema(description = "等级代码")
        private String gradeCode;

        @Schema(description = "等级名称")
        private String gradeName;

        @Schema(description = "人数")
        private Integer count;

        @Schema(description = "百分比（保留一位小数）")
        private BigDecimal percentage;

        @Schema(description = "分数线")
        private BigDecimal threshold;

        @Schema(description = "是否可调整")
        private Boolean adjustable;
    }

    /**
     * 市州统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "市州统计信息")
    public static class CityStatistics {

        @Schema(description = "市州名称")
        private String szsmc;

        @Schema(description = "人数")
        private Integer count;

        @Schema(description = "百分比（保留一位小数）")
        private BigDecimal percentage;

        @Schema(description = "平均分")
        private BigDecimal avgScore;

        @Schema(description = "等级分布")
        private Map<String, Integer> gradeDistribution;
    }

    /**
     * 分数段数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分数段数据")
    public static class ScoreSegmentData {

        @Schema(description = "分数")
        private BigDecimal score;

        @Schema(description = "该分数人数")
        private Integer count;

        @Schema(description = "累计人数")
        private Integer cumulativeCount;

        @Schema(description = "累计百分比（保留一位小数）")
        private BigDecimal cumulativePercentage;

        @Schema(description = "等级")
        private String grade;

        @Schema(description = "排名位次")
        private Integer rank;
    }
}