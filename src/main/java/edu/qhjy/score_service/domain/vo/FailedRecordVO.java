package edu.qhjy.score_service.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 匹配失败记录视图对象
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Data
@Accessors(chain = true)
public class FailedRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 阅卷序号
     */
    private String yjxh;

    /**
     * 考生姓名（来自DBF文件）
     */
    private String ksxm;

    /**
     * 总成绩
     */
    private BigDecimal zcj;

    /**
     * 客观题成绩
     */
    private BigDecimal kgtcj;

    /**
     * 主观题成绩
     */
    private BigDecimal zgtcj;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 科目名称
     */
    private String kmmc;

    /**
     * 失败类型
     * 1: YJXH在yjxh表中不存在
     * 2: YJXH存在但KSXM姓名不匹配
     */
    private Integer failureType;

    /**
     * 失败原因描述
     */
    private String failureReason;

    /**
     * 数据库中的考生姓名（当failureType=2时有值）
     */
    private String dbKsxm;

    /**
     * 数据库中的考生号（当failureType=2时有值）
     */
    private String dbKsh;

    /**
     * 行号（用于定位DBF文件中的位置）
     */
    private Integer rowNumber;

    /**
     * 建议处理方式
     */
    private String suggestion;

    /**
     * 设置YJXH不存在的失败信息
     */
    public static FailedRecordVO createYjxhNotFoundFailure(String yjxh, String ksxm, String ksjhdm, Integer rowNumber) {
        return new FailedRecordVO()
                .setYjxh(yjxh)
                .setKsxm(ksxm)
                .setKsjhdm(ksjhdm)
                .setRowNumber(rowNumber)
                .setFailureInfo(1, "阅卷序号在yjxh表中不存在", "请检查阅卷序号是否正确，或联系管理员确认yjxh表数据");
    }

    /**
     * 设置姓名不匹配的失败信息
     */
    public static FailedRecordVO createNameMismatchFailure(String yjxh, String ksxm, String dbKsxm, String dbKsh,
                                                           String ksjhdm, Integer rowNumber) {
        return new FailedRecordVO()
                .setYjxh(yjxh)
                .setKsxm(ksxm)
                .setDbKsxm(dbKsxm)
                .setDbKsh(dbKsh)
                .setKsjhdm(ksjhdm)
                .setRowNumber(rowNumber)
                .setFailureInfo(2, "考生姓名与数据库中的姓名不匹配", "姓名不匹配但成绩已导入，数据库中该阅卷序号对应的姓名为: " + dbKsxm + "，请核实考生信息");
    }

    /**
     * 创建YJXH不存在的失败记录（简化版）
     */
    public static FailedRecordVO createYjxhNotFound(edu.qhjy.score_service.domain.dto.DbfRecordDTO record) {
        return createYjxhNotFoundFailure(record.getYjxh(), record.getKsxm(), record.getKsjhdm(), record.getRowNumber())
                .setZcj(record.getZcj() != null ? new BigDecimal(record.getZcj()) : null)
                .setKgtcj(record.getKgtcj())
                .setZgtcj(record.getZgtcj())
                .setKmmc(record.getKmmc());
    }

    /**
     * 创建姓名不匹配的失败记录（简化版）
     */
    public static FailedRecordVO createNameMismatch(edu.qhjy.score_service.domain.dto.DbfRecordDTO record,
                                                    edu.qhjy.score_service.domain.entity.YjxhEntity yjxhEntity) {
        return createNameMismatchFailure(record.getYjxh(), record.getKsxm(), yjxhEntity.getKsxm(), yjxhEntity.getKsh(),
                record.getKsjhdm(), record.getRowNumber())
                .setZcj(record.getZcj() != null ? new BigDecimal(record.getZcj()) : null)
                .setKgtcj(record.getKgtcj())
                .setZgtcj(record.getZgtcj())
                .setKmmc(record.getKmmc());
    }

    /**
     * 设置失败类型和原因
     */
    public FailedRecordVO setFailureInfo(Integer failureType, String failureReason, String suggestion) {
        this.failureType = failureType;
        this.failureReason = failureReason;
        this.suggestion = suggestion;
        return this;
    }
}