package edu.qhjy.score_service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评分轨迹导入DTO
 * 用于DBF文件字段到数据库字段的映射
 *
 * @author system
 * @since 2024-01-01
 */
@Data
@Accessors(chain = true)
public class PfgjImportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 考试计划代码（参数传入）
     */
    @NotBlank(message = "考试计划代码不能为空")
    private String ksjhdm;

    /**
     * 流水号（DBF字段：lsh）
     */
    @NotBlank(message = "流水号不能为空")
    private String lsh;

    /**
     * 阅卷序号（DBF字段：ksh）
     */
    @NotNull(message = "阅卷序号不能为空")
    private Long yjxh;

    /**
     * 大题号（DBF字段：itemid）
     */
    @NotNull(message = "大题号不能为空")
    private Integer itemid;

    /**
     * 密号（DBF字段：mh）
     */
    private String mh;

    /**
     * 大题分数（DBF字段：marksum）
     */
    private String marksum;

    /**
     * 小题分值串（DBF字段：submark）
     */
    private String submark;

    /**
     * 评次（DBF字段：tasktype）
     * 1:一评, 2:二评, 3:三评, 21:异常, 22:仲裁, 23:雷同, 24:重评, 25:终评
     */
    @NotBlank(message = "评次不能为空")
    private String tasktype;

    /**
     * 评卷用户号（DBF字段：teacherid）
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
     * 获取评次描述
     *
     * @return 评次描述
     */
    public String getTasktypeDescription() {
        if (tasktype == null || tasktype.trim().isEmpty()) {
            return "未知";
        }
        return switch (tasktype.trim()) {
            case "1" -> "一评";
            case "2" -> "二评";
            case "3" -> "三评";
            case "21" -> "异常";
            case "22" -> "仲裁";
            case "23" -> "雷同";
            case "24" -> "重评";
            case "25" -> "终评";
            default -> "未知(" + tasktype + ")";
        };
    }

    /**
     * 验证评次是否有效
     *
     * @return 是否有效
     */
    public boolean isValidTasktype() {
        if (tasktype == null || tasktype.trim().isEmpty()) {
            return false;
        }
        String trimmedTasktype = tasktype.trim();
        return "1".equals(trimmedTasktype) || "2".equals(trimmedTasktype) || "3".equals(trimmedTasktype) ||
                "21".equals(trimmedTasktype) || "22".equals(trimmedTasktype) || "23".equals(trimmedTasktype) ||
                "24".equals(trimmedTasktype) || "25".equals(trimmedTasktype);
    }

    /**
     * 验证必填字段
     *
     * @return 验证结果
     */
    public boolean isValid() {
        return ksjhdm != null && !ksjhdm.trim().isEmpty() &&
                lsh != null && !lsh.trim().isEmpty() &&
                yjxh != null &&
                itemid != null &&
                tasktype != null && !tasktype.trim().isEmpty() && isValidTasktype();
    }

    /**
     * 获取验证错误信息
     *
     * @return 错误信息
     */
    public String getValidationError() {
        if (ksjhdm == null || ksjhdm.trim().isEmpty()) {
            return "考试计划代码不能为空";
        }
        if (lsh == null || lsh.trim().isEmpty()) {
            return "流水号不能为空";
        }
        if (yjxh == null) {
            return "阅卷序号不能为空";
        }
        if (itemid == null) {
            return "大题号不能为空";
        }
        if (tasktype == null || tasktype.trim().isEmpty()) {
            return "评次不能为空";
        }
        if (!isValidTasktype()) {
            return "评次值无效，有效值为：1,2,3,21,22,23,24,25";
        }
        return null;
    }
}