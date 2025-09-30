package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成绩构成使用情况VO
 * 用于展示分项配置在各考试计划中的使用情况
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩构成使用情况")
public class ScoreCompositionUsageVO {

    @Schema(description = "科目名称")
    private String kmmc;

    @Schema(description = "分项数量")
    private Integer itemCount;

    @Schema(description = "使用该配置的考试计划列表")
    private List<PlanUsageInfo> usedInPlans;

    @Schema(description = "是否可以修改（未被使用时才能修改）")
    private Boolean canModify;

    /**
     * 考试计划使用信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "考试计划使用信息")
    public static class PlanUsageInfo {

        @Schema(description = "考试计划代码")
        private String ksjhdm;

        @Schema(description = "考试计划名称")
        private String ksjhmc;

        @Schema(description = "学生数据统计")
        private StudentDataStatistics studentDataStatistics;
    }

    /**
     * 学生数据统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "学生数据统计")
    public static class StudentDataStatistics {

        @Schema(description = "总学生数")
        private Integer totalStudents;

        @Schema(description = "有分项成绩的学生数")
        private Integer studentsWithScores;

        @Schema(description = "完整分项成绩的学生数")
        private Integer studentsWithCompleteScores;
    }
}