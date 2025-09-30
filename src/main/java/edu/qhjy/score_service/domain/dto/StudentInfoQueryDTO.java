package edu.qhjy.score_service.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 考籍信息查询DTO
 *
 * @author dadalv
 * @since 2025-08-15
 */
@Data
@ApiModel(description = "考籍信息查询DTO")
public class StudentInfoQueryDTO {

    @ApiModelProperty(value = "考生号", required = true, example = "2024001001")
    @NotBlank(message = "考生号不能为空")
    private String ksh;
}