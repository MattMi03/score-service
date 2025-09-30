package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学科报名信息表实体类
 */
@Data
public class XkbmxxEntity {

    /**
     * 学考报名标识id
     */
    private Long xkbmbs;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 考生号
     */
    private String ksh;

    /**
     * 报名费减免（0：不减免；1：减免）
     */
    private Boolean bmfjm;

    /**
     * 报名表地址 (文件路径或URL)
     */
    private String bmbdz;

    /**
     * 支付状态（0：未支付；1：已支付）
     */
    private String zfzt;

    /**
     * 缴费时间
     */
    private LocalDateTime jfsj;

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