package edu.qhjy.score_service.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DBF文件导入响应视图对象
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Data
@Accessors(chain = true)
public class DbfImportResponseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 导入是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

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
     * 导入开始时间
     */
    private LocalDateTime startTime;

    /**
     * 导入结束时间
     */
    private LocalDateTime endTime;

    /**
     * 导入耗时（毫秒）
     */
    private Long duration;

    /**
     * 文件总记录数
     */
    private Integer totalRecords;

    /**
     * 成功导入记录数
     */
    private Integer successRecords;

    /**
     * 失败记录数
     */
    private Integer failedRecordsCount;

    /**
     * YJXH匹配失败记录数
     */
    private Integer yjxhNotFoundCount;

    /**
     * 姓名匹配失败记录数
     */
    private Integer nameMismatchCount;

    /**
     * 数据验证失败记录数
     */
    private Integer validationFailedCount;

    /**
     * 匹配失败的记录详情
     */
    private List<FailedRecordVO> failedRecords;

    /**
     * 成功导入的数据预览（前10条）
     */
    private List<StudentDataVO> successPreview;

    /**
     * 错误详情（如果导入过程中发生异常）
     */
    private String errorDetail;

    /**
     * 创建成功响应
     */
    public static DbfImportResponseVO success(String message) {
        return new DbfImportResponseVO()
                .setSuccess(true)
                .setMessage(message);
    }

    /**
     * 创建失败响应
     */
    public static DbfImportResponseVO failure(String message, String errorDetail) {
        return new DbfImportResponseVO()
                .setSuccess(false)
                .setMessage(message)
                .setErrorDetail(errorDetail);
    }

    /**
     * 创建失败响应（带参数）
     */
    public static DbfImportResponseVO createFailureResponse(String fileName, String message, long fileSize,
                                                            String ksjhdm, LocalDateTime startTime) {
        return new DbfImportResponseVO()
                .setSuccess(false)
                .setFileName(fileName)
                .setMessage(message)
                .setFileSize(fileSize)
                .setKsjhdm(ksjhdm)
                .setStartTime(startTime)
                .setEndTime(LocalDateTime.now())
                .calculateDuration();
    }

    /**
     * 创建失败响应（带参数，包含ksjhmc）
     */
    public static DbfImportResponseVO createFailureResponse(String fileName, String message, long fileSize,
                                                            String ksjhdm, String ksjhmc, LocalDateTime startTime) {
        return new DbfImportResponseVO()
                .setSuccess(false)
                .setFileName(fileName)
                .setMessage(message)
                .setFileSize(fileSize)
                .setKsjhdm(ksjhdm)
                .setKsjhmc(ksjhmc)
                .setStartTime(startTime)
                .setEndTime(LocalDateTime.now())
                .calculateDuration();
    }

    /**
     * 创建成功响应（带参数）
     */
    public static DbfImportResponseVO createSuccessResponse(String fileName, long fileSize, String ksjhdm, String kmmc,
                                                            LocalDateTime startTime) {
        return new DbfImportResponseVO()
                .setSuccess(true)
                .setFileName(fileName)
                .setMessage("DBF文件导入成功")
                .setFileSize(fileSize)
                .setKsjhdm(ksjhdm)
                .setKmmc(kmmc)
                .setStartTime(startTime)
                .setEndTime(LocalDateTime.now())
                .calculateDuration();
    }

    /**
     * 创建成功响应（带参数，包含ksjhmc）
     */
    public static DbfImportResponseVO createSuccessResponse(String fileName, long fileSize, String ksjhdm,
                                                            String ksjhmc, String kmmc,
                                                            LocalDateTime startTime) {
        return new DbfImportResponseVO()
                .setSuccess(true)
                .setFileName(fileName)
                .setMessage("DBF文件导入成功")
                .setFileSize(fileSize)
                .setKsjhdm(ksjhdm)
                .setKsjhmc(ksjhmc)
                .setKmmc(kmmc)
                .setStartTime(startTime)
                .setEndTime(LocalDateTime.now())
                .calculateDuration();
    }

    /**
     * 计算导入耗时
     */
    public DbfImportResponseVO calculateDuration() {
        if (startTime != null && endTime != null) {
            this.duration = java.time.Duration.between(startTime, endTime).toMillis();
        }
        return this;
    }

    /**
     * 设置导入统计信息
     */
    public DbfImportResponseVO setStatistics(Integer totalRecords, Integer successRecords,
                                             Integer yjxhNotFoundCount, Integer nameMismatchCount,
                                             Integer validationFailedCount) {
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.yjxhNotFoundCount = yjxhNotFoundCount;
        this.nameMismatchCount = nameMismatchCount;
        this.validationFailedCount = validationFailedCount;
        this.failedRecordsCount = yjxhNotFoundCount + nameMismatchCount + validationFailedCount;
        return this;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 获取成功记录数
     */
    public Integer getSuccessCount() {
        return successRecords != null ? successRecords : 0;
    }

    /**
     * 获取失败记录数
     */
    public Integer getFailedCount() {
        return failedRecords != null ? failedRecords.size() : 0;
    }

    /**
     * 设置错误详情
     */
    public DbfImportResponseVO setErrorDetails(String errorDetails) {
        this.errorDetail = errorDetails;
        return this;
    }

    /**
     * 设置失败记录数（重载方法）
     */
    public DbfImportResponseVO setFailedRecordsCount(Integer failedRecordsCount) {
        this.failedRecordsCount = failedRecordsCount;
        return this;
    }

    /**
     * 设置成功数据预览
     */
    public DbfImportResponseVO setSuccessPreview(List<StudentDataVO> successPreview) {
        this.successPreview = successPreview;
        return this;
    }

    /**
     * 获取成功率百分比
     */
    public Double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (double) successRecords / totalRecords * 100;
    }

    /**
     * 获取格式化的导入摘要
     */
    public String getImportSummary() {
        if (totalRecords == null) {
            return "导入失败";
        }
        return String.format("总计 %d 条记录，成功 %d 条，失败 %d 条（YJXH不存在: %d，姓名不匹配: %d，数据验证失败: %d）",
                totalRecords, successRecords, failedRecordsCount,
                yjxhNotFoundCount, nameMismatchCount, validationFailedCount);
    }
}