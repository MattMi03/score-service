package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考生报考科目表实体类
 */
@Data
public class XkbmkmEntity {

    /**
     * 学考报名科目标识id
     */
    private Long xkbmkmbs;

    /**
     * 学考报名标识id (关联XKBMXX表)
     */
    private Long xkbmbs;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 报名费
     */
    private Integer bmf;

    /**
     * 阅卷序号
     */
    private String yjxh;

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