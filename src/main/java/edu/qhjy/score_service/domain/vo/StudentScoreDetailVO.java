package edu.qhjy.score_service.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生成绩详情VO
 */
@Data
public class StudentScoreDetailVO {

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
     * 班级名称
     */
    private String bjmc;

    /**
     * 学校名称
     */
    private String xxmc;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 总成绩
     */
    private BigDecimal fslkscj;

    /**
     * 等级成绩码
     */
    private String cjdjm;

    /**
     * 合格码
     */
    private String cjhgm;

    /**
     * 分项成绩列表
     */
    private List<SubScoreVO> subScores;

    /**
     * 创建时间
     */
    private LocalDateTime cjsj;

    /**
     * 更新时间
     */
    private LocalDateTime gxsj;

    /**
     * 分项成绩VO
     */
    @Data
    public static class SubScoreVO {
        /**
         * 分项名称
         */
        private String fxmc;

        /**
         * 分项得分
         */
        private Integer ksfxdf;

        /**
         * 分项满分
         */
        private Integer fxcjfs;
    }
}