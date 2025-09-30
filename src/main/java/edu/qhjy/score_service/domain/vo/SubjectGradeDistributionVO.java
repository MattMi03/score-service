package edu.qhjy.score_service.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 科目成绩等级分布VO（用于饼图）
 */
@Data
public class SubjectGradeDistributionVO {

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 考试计划代码
     */
    private String examPlanCode;

    /**
     * 区域筛选条件
     */
    private String areaFilter;

    /**
     * 总人数
     */
    private Integer totalCount;

    /**
     * 等级分布数据
     */
    private List<GradeDistributionData> gradeDistribution;

    /**
     * 分数段分布数据
     */
    private List<ScoreRangeDistributionData> scoreRangeDistribution;

    /**
     * 等级分布数据
     */
    @Data
    public static class GradeDistributionData {
        /**
         * 等级代码（A/B/C/D/E）
         */
        private String gradeCode;

        /**
         * 等级名称
         */
        private String gradeName;

        /**
         * 人数
         */
        private Integer count;

        /**
         * 占比
         */
        private BigDecimal percentage;
    }

    /**
     * 分数段分布数据
     */
    @Data
    public static class ScoreRangeDistributionData {
        /**
         * 分数段描述（如"90-100"）
         */
        private String scoreRange;

        /**
         * 人数
         */
        private Integer count;

        /**
         * 占比
         */
        private BigDecimal percentage;
    }
}