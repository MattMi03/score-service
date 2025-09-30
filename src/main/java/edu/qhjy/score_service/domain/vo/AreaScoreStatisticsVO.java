package edu.qhjy.score_service.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 区域成绩统计VO（用于柱状图）
 */
@Data
public class AreaScoreStatisticsVO {

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 区域类型（city/county/school/class）
     */
    private String areaType;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 考试计划代码
     */
    private String examPlanCode;

    /**
     * 统计数据列表
     */
    private List<AreaStatisticsData> statisticsData;

    /**
     * 区域统计数据
     */
    @Data
    public static class AreaStatisticsData {
        /**
         * 区域名称
         */
        private String areaName;

        /**
         * 平均分
         */
        private BigDecimal averageScore;

        /**
         * 最高分
         */
        private BigDecimal maxScore;

        /**
         * 最低分
         */
        private BigDecimal minScore;

        /**
         * 总人数
         */
        private Integer totalCount;

        /**
         * 及格人数
         */
        private Integer passCount;

        /**
         * 及格率
         */
        private BigDecimal passRate;
    }
}