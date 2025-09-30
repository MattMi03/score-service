package edu.qhjy.score_service.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 阅卷序号实体类
 * 对应数据库表：yjxh
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class YjxhEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 阅卷序号
     */
    private String yjxh;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 考试计划名称
     */
    private String ksjhmc;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 科目类型，0：合格性考试科目；1：考察性考试科目
     */
    private Integer kmlx;

    /**
     * 考生号
     */
    private String ksh;

    /**
     * 考生姓名
     */
    private String ksxm;
}