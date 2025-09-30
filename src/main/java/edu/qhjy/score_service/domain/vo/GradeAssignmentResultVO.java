package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 等级划分结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "等级划分结果")
public class GradeAssignmentResultVO {

    @Schema(description = "处理状态", example = "SUCCESS")
    private String status;

    @Schema(description = "处理消息", example = "等级划分完成")
    private String message;

    @Schema(description = "考试计划代码", example = "202503")
    private String ksjhdm;

    @Schema(description = "科目名称", example = "数学")
    private String kmmc;

    @Schema(description = "处理的市州数量", example = "5")
    private Integer processedCityCount;

    @Schema(description = "处理的学生总数", example = "50000")
    private Integer processedStudentCount;

    @Schema(description = "处理耗时（毫秒）", example = "12500")
    private Long processingTimeMs;

    @Schema(description = "各市州等级分布统计")
    private List<CityGradeStatistics> cityGradeStatistics;

    /**
     * 市州等级统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "市州等级统计")
    public static class CityGradeStatistics {

        @Schema(description = "市州名称", example = "海东市")
        private String szsmc;

        @Schema(description = "总人数", example = "10000")
        private Integer totalCount;

        @Schema(description = "各等级人数分布")
        private Map<String, Integer> gradeDistribution;

        @Schema(description = "各等级分数线")
        private Map<String, Double> gradeThresholds;

        @Schema(description = "合格人数", example = "9800")
        private Integer qualifiedCount;

        @Schema(description = "合格率", example = "98.0")
        private Double qualifiedRate;
    }
}