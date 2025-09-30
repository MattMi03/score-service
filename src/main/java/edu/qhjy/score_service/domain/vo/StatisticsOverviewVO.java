package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统计分析概览VO
 * 用于提供统计分析的基本信息和可用选项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计分析概览数据")
public class StatisticsOverviewVO {

    @Schema(description = "可用的考试计划列表")
    private List<String> availableExamPlans;

    @Schema(description = "可用的城市列表")
    private List<String> availableCities;

    @Schema(description = "支持的区域级别")
    private List<String> supportedAreaLevels;

    @Schema(description = "支持的图表类型")
    private List<String> supportedChartTypes;

    @Schema(description = "统计信息")
    private StatisticsInfo statisticsInfo;

    /**
     * 统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "统计信息")
    public static class StatisticsInfo {

        @Schema(description = "总考试计划数")
        private Integer totalExamPlans;

        @Schema(description = "总科目数")
        private Integer totalSubjects;

        @Schema(description = "总区域数")
        private Integer totalAreas;

        @Schema(description = "数据更新时间")
        private String lastUpdateTime;
    }
}