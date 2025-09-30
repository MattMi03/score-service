package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 毕业生统计响应VO
 *
 * @author system
 * @date 2025-01-15
 */
@Data
@Schema(description = "毕业生统计响应VO")
public class GraduationStatisticsVO {

    @Schema(description = "毕业年度", example = "2025")
    private Integer bynd;

    @Schema(description = "级别", example = "2022")
    private Integer rxnd;

    @Schema(description = "各市州毕业生统计数据")
    private List<CityStatistics> graduationStudentStatics;

    @Schema(description = "毕业年度总人数")
    private Integer byndTotalCount;

    @Schema(description = "级别总人数")
    private Integer rxndTotalCount;

    /**
     * 市州统计数据
     */
    @Data
    @Schema(description = "市州统计数据")
    public static class CityStatistics {

        @Schema(description = "市州名称", example = "西宁市")
        private String szsmc;

        @Schema(description = "该市州毕业生人数", example = "1500")
        private Integer totalCount;
    }
}