package edu.qhjy.score_service.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 成绩保存DTO
 *
 * @author dadalv
 * @since 2025-08-15
 */
@Data
@ApiModel(description = "成绩保存DTO")
public class ScoreSaveDTO {

    @ApiModelProperty(value = "考生号", required = true, example = "2024001001")
    @NotBlank(message = "考生号不能为空")
    private String ksh;

    @ApiModelProperty(value = "身份证件号", required = true, example = "110101199001011234")
    @NotBlank(message = "身份证件号不能为空")
    private String sfzjh;

    @ApiModelProperty(value = "姓名", required = true, example = "张三")
    @NotBlank(message = "姓名不能为空")
    private String xm;

    @ApiModelProperty(value = "成绩信息", required = true, example = "{\"语文\":\"合格\",\"数学\":\"不合格\",\"外语\":null}")
    @NotNull(message = "成绩信息不能为空")
    private Map<String, String> scores;
}