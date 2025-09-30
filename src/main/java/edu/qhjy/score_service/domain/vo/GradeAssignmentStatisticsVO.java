package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 等级赋分统计VO
 */
@Data
@Schema(description = "等级赋分统计")
public class GradeAssignmentStatisticsVO {

    @Schema(description = "考试计划代码")
    private String ksjhdm;

    @Schema(description = "科目名称")
    private String kmmc;

    @Schema(description = "总学生数")
    private int totalStudents;

    @Schema(description = "已赋分学生数")
    private int assignedStudents;

    @Schema(description = "等级分布")
    private Map<String, Integer> gradeDistribution;

    @Schema(description = "等级比例")
    private Map<String, Double> gradePercentages;

    @Schema(description = "城市统计列表")
    private List<CityStatistics> cityStatistics;

    @Data
    @Schema(description = "城市统计")
    public static class CityStatistics {
        @Schema(description = "市州名称")
        private String szsmc;

        @Schema(description = "学生数")
        private int studentCount;

        @Schema(description = "等级分布")
        private Map<String, Integer> gradeDistribution;

        @Schema(description = "等级阈值")
        private Map<String, Double> gradeThresholds;
    }
}