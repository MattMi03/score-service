package edu.qhjy.score_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 等级临界值DTO
 * 存储各等级的分数临界值和人数信息
 */
@Data
public class GradeThresholdsDTO {

    /**
     * 市州名称
     */
    private String szsmc;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 总人数
     */
    private Integer totalCount;

    /**
     * A等级最低分数
     */
    private BigDecimal gradeAThreshold;

    /**
     * A等级人数
     */
    private Integer gradeACount;

    /**
     * B等级最低分数
     */
    private BigDecimal gradeBThreshold;

    /**
     * B等级人数
     */
    private Integer gradeBCount;

    /**
     * C等级最低分数
     */
    private BigDecimal gradeCThreshold;

    /**
     * C等级人数
     */
    private Integer gradeCCount;

    /**
     * D等级最低分数
     */
    private BigDecimal gradeDThreshold;

    /**
     * D等级人数
     */
    private Integer gradeDCount;

    /**
     * E等级最低分数
     */
    private BigDecimal gradeEThreshold;

    /**
     * E等级人数
     */
    private Integer gradeECount;

    /**
     * 各等级详细信息
     * key: 等级代码(A/B/C/D/E)
     * value: Map包含threshold(分数线)和count(人数)
     */
    private Map<String, Map<String, Object>> gradeDetails;

    /**
     * 获取指定等级的分数线
     */
    public BigDecimal getThresholdByGrade(String grade) {
        return switch (grade.toUpperCase()) {
            case "A" -> gradeAThreshold;
            case "B" -> gradeBThreshold;
            case "C" -> gradeCThreshold;
            case "D" -> gradeDThreshold;
            case "E" -> gradeEThreshold;
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * 获取指定等级的人数
     */
    public Integer getCountByGrade(String grade) {
        return switch (grade.toUpperCase()) {
            case "A" -> gradeACount;
            case "B" -> gradeBCount;
            case "C" -> gradeCCount;
            case "D" -> gradeDCount;
            case "E" -> gradeECount;
            default -> 0;
        };
    }
}