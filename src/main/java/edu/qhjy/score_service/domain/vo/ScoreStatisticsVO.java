package edu.qhjy.score_service.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 成绩统计信息VO
 */
@Data
public class ScoreStatisticsVO {

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
     * 标准差
     */
    private BigDecimal standardDeviation;

    /**
     * 各分数段人数分布
     * key: 分数段描述（如"90-100", "80-89"等）
     * value: 该分数段的人数
     */
    private Map<String, Integer> scoreDistribution;

    /**
     * 合格人数
     */
    private Integer passCount;

    /**
     * 不合格人数
     */
    private Integer failCount;

    /**
     * 合格率
     */
    private BigDecimal passRate;
}