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
 * 等级调整结果视图对象
 * 用于返回等级调整的预览和确认结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "等级调整结果")
public class GradeAdjustmentResultVO {

    @Schema(description = "考试计划代码", example = "2024001")
    private String ksjhdm;

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "市州名称，null表示全省", example = "长沙市")
    private String szsmc;

    @Schema(description = "调整是否成功", example = "true")
    private Boolean success;

    @Schema(description = "警告级别：NONE(无警告), WARNING(黄色警告), DANGER(红色警告)", example = "WARNING", allowableValues = {
            "NONE", "WARNING", "DANGER"})
    private String warningLevel;

    @Schema(description = "警告信息描述", example = "检测到分数线调整幅度较大（最大调整3.5分），请确认调整的合理性")
    private String warningMessage;

    @Schema(description = "原始等级分布数据列表")
    private List<GradeDistributionData> originalDistribution;

    @Schema(description = "调整后等级分布数据列表")
    private List<GradeDistributionData> adjustedDistribution;

    @Schema(description = "等级变化统计信息")
    private GradeChangeStatistics changeStatistics;

    @Schema(description = "受影响的学生总数", example = "1250")
    private Integer affectedStudentCount;

    @Schema(description = "处理时间（毫秒）", example = "1250")
    private Long processingTime;

    @Schema(description = "操作时间", example = "2024-03-15 14:30:25")
    private String operationTime;

    /**
     * 等级分布数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "等级分布数据")
    public static class GradeDistributionData {

        @Schema(description = "等级代码", example = "A", allowableValues = {"A", "B", "C", "D", "E"})
        private String grade;

        @Schema(description = "分数线", example = "85.5")
        private BigDecimal threshold;

        @Schema(description = "该等级人数", example = "1250")
        private Integer count;

        @Schema(description = "该等级占比（保留一位小数）", example = "15.2")
        private BigDecimal percentage;

        @Schema(description = "累计人数", example = "3500")
        private Integer cumulativeCount;

        @Schema(description = "分数线变化量（正数表示提高，负数表示降低）", example = "2.5")
        private BigDecimal thresholdChange;

        @Schema(description = "人数变化量（正数表示增加，负数表示减少）", example = "-30")
        private Integer countChange;
    }

    /**
     * 等级变化统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "等级变化统计")
    public static class GradeChangeStatistics {

        @Schema(description = "各等级变化数量，键为等级代码，值为变化人数", example = "{\"A\": -30, \"B\": 45, \"C\": -15, \"D\": 0, \"E\": 0}")
        private Map<String, Integer> gradeChanges;

        @Schema(description = "总受影响学生数（等级发生变化的学生总数）", example = "90")
        private Integer totalAffected;

        @Schema(description = "等级提升的学生数（从低等级升到高等级）", example = "45")
        private Integer upgradeCount;

        @Schema(description = "等级降低的学生数（从高等级降到低等级）", example = "45")
        private Integer downgradeCount;

        @Schema(description = "最大分数线调整幅度（绝对值）", example = "3.5")
        private BigDecimal maxThresholdAdjustment;

        @Schema(description = "平均分数线调整幅度（绝对值）", example = "2.1")
        private BigDecimal avgThresholdAdjustment;

        @Schema(description = "详细变化信息描述", example = "{\"summary\": \"A等级减少30人，B等级增加45人\", \"impact\": \"中等影响\"}")
        private Map<String, String> changeDetails;

        // 兼容性方法
        public Integer getUpgradedStudents() {
            return upgradeCount;
        }

        public Integer getDowngradedStudents() {
            return downgradeCount;
        }
    }
}