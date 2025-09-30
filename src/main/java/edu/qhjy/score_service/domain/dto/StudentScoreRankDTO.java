package edu.qhjy.score_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学生成绩排名DTO
 * 用于等级划分计算过程中的数据传输
 */
@Data
public class StudentScoreRankDTO {

    /**
     * 考试成绩标识id
     */
    private Long kscjbs;

    /**
     * 考生号
     */
    private String ksh;

    /**
     * 考生姓名
     */
    private String ksxm;

    /**
     * 所在市州名称
     */
    private String szsmc;

    /**
     * 分数类考试成绩
     */
    private BigDecimal fslkscj;

    /**
     * 排名位次（从1开始）
     */
    private Integer rankNum;

    /**
     * 总人数
     */
    private Integer totalCount;

    /**
     * 计算得出的等级
     */
    private String grade;

    /**
     * 是否合格
     */
    private String qualified;
}