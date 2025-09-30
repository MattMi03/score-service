package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考试科目信息表实体类
 */
@Data
public class KskmxxEntity {

    /**
     * 考试计划代码 (复合主键)
     */
    private String ksjhdm;

    /**
     * 考试计划名称
     */
    private String ksjhmc;

    /**
     * 科目代码 (复合主键)
     */
    private String kmdm;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 科目类型 (0:合格性科目考试，1:考查性科目考试)
     */
    private Integer kmlx;

    /**
     * 考试日期
     */
    private LocalDate ksrq;

    /**
     * 考试开始时间
     */
    private LocalTime kskssj;

    /**
     * 考试结束时间
     */
    private LocalTime ksjssj;

    /**
     * 报名费
     */
    private BigDecimal bmf;

    /**
     * 考场类型 (0:笔试科目考场; 1:机考考场; 2:物理实验考场; 3:化学实验考场; 4:生物实验考场; 5:体育实践考场; 6:其他考场)
     */
    private Integer kclx;

    /**
     * 级别，就读年级
     */
    private Integer jb;

    /**
     * 审核状态
     */
    private String shzt;

    /**
     * 审核时间
     */
    private LocalDateTime shsj;

    /**
     * 审核人姓名
     */
    private String shrxm;

    /**
     * 审核意见
     */
    private String shyj;

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