package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考试计划实体类
 * 对应数据库表：KSJH (edu_exam库)
 */
@Data
public class KsjhEntity {

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 考试计划名称
     */
    private String ksjhmc;

    /**
     * 报名开始日期
     */
    private LocalDate bmksrq;

    /**
     * 报名截止日期
     */
    private LocalDate bmjzrq;

    /**
     * 发布时间
     */
    private LocalDateTime fbsj;

    /**
     * 结束时间
     */
    private LocalDateTime jssj;

    /**
     * 发布状态
     */
    private Integer fbzt;

    /**
     * 创建人姓名
     */
    private String cjrxm;

    /**
     * 创建人工作人员码
     */
    private String cjrgzrym;

    /**
     * 创建时间
     */
    private LocalDateTime cjsj;

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
}