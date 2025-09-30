package edu.qhjy.score_service.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 成绩等第册响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩等第册响应数据")
public class GradeBookVO {

    @Schema(description = "考区名称")
    private String kqmc;

    @Schema(description = "学校名称")
    private String xxmc;

    @Schema(description = "学生数据列表")
    @JsonProperty("studentData")
    private List<StudentGradeData> studentData;

    /**
     * 学生成绩数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "学生成绩数据")
    public static class StudentGradeData {

        @Schema(description = "考籍号")
        private String ksh;

        @Schema(description = "姓名")
        private String xm;

        @Schema(description = "身份证件号")
        private String sfzjh;

        @Schema(description = "性别")
        private String xb;

        @Schema(description = "成绩字符串", example = "地理(A)   化学(B)   历史(C)   通用技术(合格)   信息技术(不合格)")
        private String scores;
    }
}