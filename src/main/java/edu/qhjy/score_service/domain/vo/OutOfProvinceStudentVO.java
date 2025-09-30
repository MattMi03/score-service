package edu.qhjy.score_service.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 省外转入考生查询VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "省外转入考生查询响应数据")
public class OutOfProvinceStudentVO {

    /**
     * 审核状态
     */
    @Schema(description = "审核状态", example = "已审核")
    private String shzt;

    /**
     * 审核时间
     */
    @Schema(description = "审核时间", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shsj;

    /**
     * 审核人姓名
     */
    @Schema(description = "审核人姓名", example = "张三")
    private String shr;

    /**
     * 姓名
     */
    @Schema(description = "姓名", example = "李四")
    private String xm;

    /**
     * 身份证件号
     */
    @Schema(description = "身份证件号", example = "430102199001011234")
    private String sfzjh;

    /**
     * 省市名称
     */
    @Schema(description = "省市名称", example = "长沙市")
    private String ds;

    /**
     * 市县名称
     */
    @Schema(description = "市县名称", example = "岳麓区")
    private String kq;

    /**
     * 学校名称
     */
    @Schema(description = "学校名称", example = "长沙市第一中学")
    private String school;

    /**
     * 创建人姓名
     */
    @Schema(description = "创建人姓名", example = "王五")
    private String cjr;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-10 09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cjsj;

    /**
     * 审核阶段
     */
    @Schema(description = "审核阶段", example = "初审")
    private String shjd;

    /**
     * 审核意见
     */
    @Schema(description = "审核意见", example = "材料齐全，符合转入条件")
    private String shyj;

    /**
     * 考生号
     */
    @Schema(description = "考生号", example = "2024001001")
    private String ksh;
}