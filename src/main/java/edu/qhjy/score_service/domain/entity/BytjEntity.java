package edu.qhjy.score_service.domain.entity;

import lombok.Data;

/**
 * 毕业条件设置表实体类
 * 对应数据库表：bytj
 */
@Data
public class BytjEntity {

    /**
     * 毕业条件标识
     */
    private Long bytjbs;

    /**
     * 所在市名称
     */
    private String szsmc;

    /**
     * 考试科目最低数量
     */
    private Integer kskm;

    /**
     * 考查科目最低数量
     */
    private Integer kckm;
}