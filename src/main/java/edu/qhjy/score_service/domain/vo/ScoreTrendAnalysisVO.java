package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩趋势分析VO
 * 用于折线图展示历史成绩趋势
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩趋势分析数据")
public class ScoreTrendAnalysisVO {

    @Schema(description = "科目名称")
    private String kmmc;

    @Schema(description = "区域级别(city/county/school/class)")
    private String areaLevel;

    @Schema(description = "区域代码")
    private String areaCode;

    @Schema(description = "区域名称")
    private String areaName;

    @Schema(description = "趋势数据点列表")
    private List<TrendDataPoint> trendData;

    @Schema(description = "统计摘要")
    private TrendSummary summary;

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "趋势数据点")
    public static class TrendDataPoint {

        @Schema(description = "考试计划代码")
        private String ksjhdm;

        @Schema(description = "考试年份")
        private String year;

        @Schema(description = "考试期次(如：07表示7月)")
        private String period;

        @Schema(description = "平均分")
        private BigDecimal avgScore;

        @Schema(description = "最高分")
        private BigDecimal maxScore;

        @Schema(description = "最低分")
        private BigDecimal minScore;

        @Schema(description = "参考人数")
        private Integer totalCount;

        @Schema(description = "及格人数")
        private Integer passCount;

        @Schema(description = "及格率")
        private BigDecimal passRate;

        // 添加便捷的setter方法
        public void setSubjectName(String subjectName) {
            // 兼容性方法，实际不存储
        }

        public void setAreaFilter(String areaFilter) {
            // 兼容性方法，实际不存储
        }

        public void setExamPlanCode(String examPlanCode) {
            this.ksjhdm = examPlanCode;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public BigDecimal getAverageScore() {
            return this.avgScore;
        }

        public void setAverageScore(BigDecimal averageScore) {
            this.avgScore = averageScore;
        }
    }

    /**
     * 趋势统计摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "趋势统计摘要")
    public static class TrendSummary {

        @Schema(description = "数据点总数")
        private Integer dataPointCount;

        @Schema(description = "平均分趋势(上升/下降/稳定)")
        private String avgScoreTrend;

        @Schema(description = "平均分变化幅度")
        private BigDecimal avgScoreChange;

        @Schema(description = "及格率趋势(上升/下降/稳定)")
        private String passRateTrend;

        @Schema(description = "及格率变化幅度")
        private BigDecimal passRateChange;

        @Schema(description = "最佳表现期次")
        private String bestPeriod;

        @Schema(description = "最差表现期次")
        private String worstPeriod;

        // 添加便捷的setter方法
        public void setAverageScoreChange(BigDecimal averageScoreChange) {
            this.avgScoreChange = averageScoreChange;
        }

        public void setAverageScoreTrend(String averageScoreTrend) {
            this.avgScoreTrend = averageScoreTrend;
        }

        public void setTrendSummary(TrendSummary trendSummary) {
            if (trendSummary != null) {
                this.dataPointCount = trendSummary.dataPointCount;
                this.avgScoreTrend = trendSummary.avgScoreTrend;
                this.avgScoreChange = trendSummary.avgScoreChange;
                this.passRateTrend = trendSummary.passRateTrend;
                this.passRateChange = trendSummary.passRateChange;
                this.bestPeriod = trendSummary.bestPeriod;
                this.worstPeriod = trendSummary.worstPeriod;
            }
        }
    }
}