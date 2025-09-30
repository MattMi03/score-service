package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 科目信息表实体类
 */
@Data
public class KmxxEntity {

    /**
     * 科目标识id
     */
    private Long kmbs;

    /**
     * 科目代码
     */
    private String kmdm;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 科目类型 (0:合格性考试科目，1:考察性考试科目)
     */
    private Integer kmlx;

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