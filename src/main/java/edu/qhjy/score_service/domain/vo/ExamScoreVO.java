package edu.qhjy.score_service.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考查科目成绩查询响应VO
 */
@Data
public class ExamScoreVO {

    /**
     * 审核状态
     */
    private String shzt;

    /**
     * 审核意见
     */
    private String shyj;

    /**
     * 考生号
     */
    private String ksh;

    /**
     * 姓名
     */
    private String xm;

    /**
     * 身份证号
     */
    private String sfzjh;

    /**
     * 学校名称
     */
    private String xxmc;

    /**
     * 所在市名称
     */
    private String szsmc;

    /**
     * 分数类考试成绩
     */
    private Integer fslkscj;

    /**
     * 成绩合格码
     */
    private String cjhgm;

    /**
     * 考区名称
     */
    private String kqmc;

    /**
     * 班级名称
     */
    private String bjmc;

    /**
     * 审核时间
     */
    private LocalDateTime shsj;

    /**
     * 审核人姓名
     */
    private String shrxm;

    /**
     * 审核阶段
     */
    private String shjd;
}