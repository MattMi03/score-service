package edu.qhjy.score_service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DBF文件记录数据传输对象
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Data
@Accessors(chain = true)
public class DbfRecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 阅卷序号（对应DBF中的ksh字段）
     */
    @NotBlank(message = "阅卷序号不能为空")
    private String yjxh;

    /**
     * 考生姓名（对应DBF中的xm字段）
     */
    @NotBlank(message = "考生姓名不能为空")
    private String ksxm;

    /**
     * 总成绩（对应DBF中的zcj字段）
     */
    @NotNull(message = "总成绩不能为空")
    private Integer zcj;

    /**
     * 客观题成绩（对应DBF中的kgtcj字段）
     */
    @NotNull(message = "客观题成绩不能为空")
    private BigDecimal kgtcj;

    /**
     * 主观题成绩（对应DBF中的zgtcj字段）
     */
    @NotNull(message = "主观题成绩不能为空")
    private BigDecimal zgtcj;

    /**
     * 考试计划代码（从接口参数传入）
     */
    @NotBlank(message = "考试计划代码不能为空")
    private String ksjhdm;

    /**
     * 科目名称（从文件名解析）
     */
    @NotBlank(message = "科目名称不能为空")
    private String kmmc;

    /**
     * 真实考生号（通过yjxh表匹配获得）
     */
    private String ksh;

    /**
     * 真实考生号（别名，用于兼容）
     */
    private String realKsh;

    /**
     * 行号（用于错误定位）
     */
    private Integer rowNumber;

    /**
     * 验证成绩格式是否正确（tinyint）
     */
    public boolean isValidScoreFormat(Integer score) {
        if (score == null) {
            return false;
        }
        // 检查成绩范围是否在tinyint范围内（0-255）
        return score >= 0 && score <= 255;
    }

    /**
     * 验证成绩格式是否正确（BigDecimal）
     */
    public boolean isValidScoreFormat(BigDecimal score) {
        if (score == null) {
            return false;
        }
        // 检查成绩是否为有效数值且在合理范围内
        return score.compareTo(BigDecimal.ZERO) >= 0 && score.compareTo(new BigDecimal("999.9")) <= 0;
    }

    /**
     * 验证所有成绩字段格式
     */
    public boolean isAllScoresValid() {
        return isValidScoreFormat(zcj) &&
                isValidScoreFormat(kgtcj) &&
                isValidScoreFormat(zgtcj);
    }

    /**
     * 获取真实考生号
     */
    public String getRealKsh() {
        return realKsh != null ? realKsh : ksh;
    }

    /**
     * 设置真实考生号
     */
    public DbfRecordDTO setRealKsh(String realKsh) {
        this.realKsh = realKsh;
        this.ksh = realKsh; // 同时设置ksh字段
        return this;
    }
}