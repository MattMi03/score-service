package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校基本信息表实体类
 * 对应数据库表：xxjbxx
 */
@Data
public class XxjbxxEntity {

    /**
     * 学校标识
     */
    private Long xxbs;

    /**
     * 学校代码
     */
    private String xxdm;

    /**
     * 学校名称
     */
    private String xxmc;

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