package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生数据响应VO
 * 用于前端展示学生成绩信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生数据响应")
public class StudentDataVO {

    @Schema(description = "考试成绩标识ID")
    private Long kscjbs;

    @Schema(description = "考籍号", example = "202501001")
    private String ksh;

    @Schema(description = "考生姓名", example = "张三")
    private String ksxm;

    @Schema(description = "学校名称", example = "长沙市第一中学")
    private String xxmc;

    @Schema(description = "考区名称（区县）", example = "岳麓区")
    private String kqmc;

    @Schema(description = "地市名称", example = "长沙市")
    private String szsmc;

    @Schema(description = "班级名称", example = "高三1班")
    private String bjmc;

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "考试计划代码", example = "202507001")
    private String ksjhdm;

    @Schema(description = "考试计划名称", example = "2025年7月普通高中学业水平合格性考试")
    private String ksjhmc;

    @Schema(description = "总评成绩", example = "85.5")
    private BigDecimal fslkscj;

    @Schema(description = "合格评定（合格/不合格）", example = "合格")
    private String cjhgm;

    @Schema(description = "等第（A/B/C/D/E）", example = "B")
    private String cjdjm;

    @Schema(description = "科目类型（0:合格性考试，1:考察性考试）")
    private Integer kmlx;

    @Schema(description = "开考类型名称", example = "正考")
    private String kklxmc;

    @Schema(description = "创建时间")
    private LocalDateTime cjsj;

    @Schema(description = "更新时间")
    private LocalDateTime gxsj;

    @Schema(description = "客观题成绩", example = "45.0")
    private BigDecimal kgtcj;

    @Schema(description = "主观题成绩", example = "40.5")
    private BigDecimal zgtcj;

    @Schema(description = "审核状态", example = "已审核")
    private String shzt;

    @Schema(description = "审核时间")
    private LocalDateTime shsj;

    @Schema(description = "审核人姓名", example = "张老师")
    private String shrxm;

    /**
     * 获取显示的成绩评定
     * 根据科目类型决定显示合格评定还是等第
     */
    @Schema(description = "显示的成绩评定")
    public String getDisplayGrade() {
        if (kmlx == null) {
            return null;
        }
        // KMLX=0：合格性考试，只显示等第
        if (kmlx == 0) {
            return cjdjm;
        }
        // KMLX=1：考察性考试，只显示合格评定
        else if (kmlx == 1) {
            return cjhgm;
        }
        return null;
    }

    /**
     * 获取成绩评定类型描述
     */
    @Schema(description = "成绩评定类型")
    public String getGradeType() {
        if (kmlx == null) {
            return "未知";
        }
        return kmlx == 0 ? "等第" : "合格评定";
    }

    /**
     * 判断是否有有效成绩
     */
    public boolean hasValidScore() {
        return fslkscj != null && fslkscj.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * 获取格式化的成绩
     */
    public String getFormattedScore() {
        if (fslkscj == null) {
            return "-";
        }
        return fslkscj.stripTrailingZeros().toPlainString();
    }
}