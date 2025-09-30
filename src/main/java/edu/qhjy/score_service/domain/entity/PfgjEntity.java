package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评分轨迹实体类
 *
 * @author system
 * @since 2024-01-01
 */
@Data
public class PfgjEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评分轨迹标识
     */
    private Long pfgjbs;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 流水号
     */
    private String lsh;

    /**
     * 阅卷序号
     */
    private Long yjxh;

    /**
     * 大题号
     */
    private Integer itemid;

    /**
     * 密号
     */
    private String mh;

    /**
     * 大题分数
     */
    private String marksum;

    /**
     * 小题分值串
     */
    private String submark;

    /**
     * 评次
     */
    private String tasktype;

    /**
     * 评卷用户号
     */
    private Integer teacherid;

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