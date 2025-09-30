package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生成绩VO
 * 用于毕业条件设置内部数据处理和传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生成绩数据")
public class StudentScoreVO {

    @Schema(description = "考生号", example = "202401001")
    private String ksh;

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "科目类型（0=考试科目，1=考察科目）", example = "0")
    private Integer kmlx;

    @Schema(description = "成绩合格码", example = "合格")
    private String cjhgm;

    @Schema(description = "成绩等级码", example = "A")
    private String cjdjm;

    @Schema(description = "是否合格", example = "true")
    private Boolean isPass;
}