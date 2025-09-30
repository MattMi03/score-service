package edu.qhjy.score_service.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 考籍信息查询VO
 *
 * @author system
 * @date 2024-01-15
 */
@Data
@ApiModel(description = "考籍信息查询VO")
public class StudentInfoVO {

    @ApiModelProperty(value = "考生号", example = "2024001001")
    private String ksh;

    @ApiModelProperty(value = "身份证件号", example = "110101199001011234")
    private String sfzjh;

    @ApiModelProperty(value = "姓名", example = "张三")
    private String xm;

    @ApiModelProperty(value = "科目成绩列表", example = "{\"语文\":\"合格\",\"数学\":\"不合格\",\"外语\":null}")
    private Map<String, String> scoresList;
}