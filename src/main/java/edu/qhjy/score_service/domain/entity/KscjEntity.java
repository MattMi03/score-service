package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试成绩信息表实体类 (仅存储成绩相关数据)
 */
@Data
public class KscjEntity {

    /**
     * 考试成绩标识id
     */
    private Long kscjbs;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 考试计划名称
     */
    private String ksjhmc;


    /**
     * 阅卷序号
     */
    private String yjxh;

    /**
     * 考试科目名称
     */
    private String kmmc;

    /**
     * 考籍号
     */
    private String ksh;

    /**
     * 开考类型
     */
    private String kklxmc;

    /**
     * 科目类型 (0：合格性考试科目；1：考察性考试科目)
     */
    private Integer kmlx;

    /**
     * 分数类考试成绩
     */
    private Integer fslkscj;

    /**
     * 成绩分项1得分
     */
    private BigDecimal cjfx1;

    /**
     * 成绩分项2得分
     */
    private BigDecimal cjfx2;

    /**
     * 科目等级成绩码
     */
    private String cjdjm;

    /**
     * 成绩合格码（合格、不合格）
     */
    private String cjhgm;

    /**
     * 审核阶段
     */
    private String shjd;

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
     * 创建人
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