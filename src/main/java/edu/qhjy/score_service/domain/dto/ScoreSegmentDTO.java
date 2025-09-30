package edu.qhjy.score_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 一分一段数据传输对象
 * 用于存储单个分数段的统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSegmentDTO {

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 该分数的人数
     */
    private Integer count;

    /**
     * 累计人数（从最高分到当前分数）
     */
    private Integer cumulativeCount;

    /**
     * 累计百分比（保留一位小数）
     */
    private BigDecimal cumulativePercentage;

    /**
     * 等级（A/B/C/D/E）
     */
    private String grade;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 市州名称（用于分市州统计）
     */
    private String szsmc;

    /**
     * 市州序号
     */
    private Integer szsxh;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 科目名称
     */
    private String kmmc;
}