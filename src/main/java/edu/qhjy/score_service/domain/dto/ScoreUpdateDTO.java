package edu.qhjy.score_service.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成绩更新DTO
 * 用于批量更新成绩数据
 */
@Data
public class ScoreUpdateDTO {

    /**
     * 考籍号
     */
    private String ksh;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 分数类考试成绩
     */
    private Integer fslkscj;

    /**
     * 成绩合格评定
     */
    private String cjhgm;

    /**
     * 考考类型名称
     */
    private String kklxmc;

    /**
     * 更新人姓名
     */
    private String gxrxm;

    /**
     * 更新人工作人员码
     */
    private String gxrgzrym;

    /**
     * 更新时间
     */
    private LocalDateTime gxsj;

    /**
     * 构造函数
     */
    public ScoreUpdateDTO() {
    }

    /**
     * 带参数的构造函数
     */
    public ScoreUpdateDTO(String ksh, String ksjhdm, String kmmc, Integer fslkscj, String cjhgm) {
        this.ksh = ksh;
        this.ksjhdm = ksjhdm;
        this.kmmc = kmmc;
        this.fslkscj = fslkscj;
        this.cjhgm = cjhgm;
        this.gxsj = LocalDateTime.now();
    }
}