package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 毕业生统计查询请求DTO
 *
 * @author dadalv
 * @since 2025-08-15
 */
@Data
@Schema(description = "毕业生统计查询请求DTO")
public class GraduationStatisticsQueryDTO {

    @Schema(description = "毕业年度，对应ksxx表中的BYND", example = "2025")
    @NotNull(message = "毕业年度不能为空")
    private Integer bynd;

    @Schema(description = "级别，对应ksxx表中的RXND", example = "2022")
    @NotNull(message = "级别不能为空")
    private Integer rxnd;
}