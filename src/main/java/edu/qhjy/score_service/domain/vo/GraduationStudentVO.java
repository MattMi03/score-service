package edu.qhjy.score_service.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 毕业生查询响应VO
 * 用于前端展示毕业生信息和成绩数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "毕业生查询响应")
public class GraduationStudentVO {

    @Schema(description = "毕业年度", example = "2024")
    private String bynd;

    @Schema(description = "考生号", example = "202401001")
    private String ksh;

    @Schema(description = "姓名", example = "张三")
    private String xm;

    @Schema(description = "性别", example = "男")
    private String xb;

    @Schema(description = "所在市名称", example = "西宁市")
    private String szsmc;

    @Schema(description = "考区名称", example = "城东区")
    private String kqmc;

    @Schema(description = "学校名称", example = "青海省第一中学")
    private String xxmc;

    @Schema(description = "考籍状态名称", example = "在籍")
    private String kjztmc;

    @Schema(description = "科目成绩映射，格式：{\"科目名称1\": \"成绩1\", \"科目名称2\": \"成绩2\"}")
    @Builder.Default
    private Map<String, String> scores = new HashMap<>();

    @Schema(description = "考试科目合格数量", example = "6")
    private Integer examSubjectPassCount;

    @Schema(description = "考察科目合格数量", example = "4")
    private Integer assessmentSubjectPassCount;

    @Schema(description = "毕业条件要求的考试科目最低合格数量", example = "6")
    private Integer requiredExamSubjectCount;

    @Schema(description = "必修考察科目数量", example = "4")
    private Integer requiredAssessmentSubjectCount;

    /**
     * 内部使用的毕业条件判断结果（不对外暴露）
     */
    @JsonIgnore
    private Boolean isQualifiedInternal;
}