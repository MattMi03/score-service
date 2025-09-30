package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 位次信息表实体类
 * 对应数据库表：WCXX
 */
@Data
public class WcxxEntity {

    /**
     * 位次标识id
     */
    private Long wcbs;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 考试计划名称
     */
    private String ksjhmc;

    /**
     * 等级码
     */
    private String djm;

    /**
     * 市州序号
     */
    private Integer szsxh;

    /**
     * 所在市州名称
     */
    private String szsmc;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 分数类考试成绩
     */
    private Integer fslkscj;

    /**
     * 百分比
     */
    private Integer bfb;

    /**
     * 本分段人数
     */
    private Integer bfdrs;

    /**
     * 累计百分比
     */
    private BigDecimal ljbfb;

    /**
     * 累计人数
     */
    private Integer ljrs;

    /**
     * 本等级最低分
     */
    private BigDecimal djzdf;

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