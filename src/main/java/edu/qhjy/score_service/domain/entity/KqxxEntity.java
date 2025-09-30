package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考区信息表实体类
 * 对应数据库表：kqxx
 */
@Data
public class KqxxEntity {

    /**
     * 考区标识
     */
    private Long kqbs;

    /**
     * 考区代码
     */
    private String kqdm;

    /**
     * 考区名称
     */
    private String kqmc;

    /**
     * 所属行政区划代码
     */
    private String ssxzqhdm;

    /**
     * 所属行政区划名称
     */
    private String ssxzqhmc;

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