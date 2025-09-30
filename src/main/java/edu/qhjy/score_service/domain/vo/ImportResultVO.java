package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用导入结果VO
 *
 * @author system
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "导入结果")
public class ImportResultVO {

    @Schema(description = "导入是否成功")
    private Boolean success;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "总记录数")
    private Integer totalCount;

    @Schema(description = "成功导入的记录数")
    private Integer successCount;

    @Schema(description = "失败的记录数")
    private Integer failCount;

    @Schema(description = "错误信息列表")
    private List<String> errorMessages;

    @Schema(description = "导入开始时间")
    private String startTime;

    @Schema(description = "导入结束时间")
    private String endTime;

    @Schema(description = "导入耗时（毫秒）")
    private Long duration;

    @Schema(description = "文件名")
    private String fileName;

    /**
     * 创建成功结果
     *
     * @param message      消息
     * @param totalCount   总数
     * @param successCount 成功数
     * @return 结果对象
     */
    public static ImportResultVO success(String message, Integer totalCount, Integer successCount) {
        return ImportResultVO.builder()
                .success(true)
                .message(message)
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(totalCount - successCount)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param message 错误消息
     * @return 结果对象
     */
    public static ImportResultVO failure(String message) {
        return ImportResultVO.builder()
                .success(false)
                .message(message)
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .build();
    }

    /**
     * 创建失败结果（带总数）
     *
     * @param message    错误消息
     * @param totalCount 总数
     * @return 结果对象
     */
    public static ImportResultVO failure(String message, Integer totalCount) {
        return ImportResultVO.builder()
                .success(false)
                .message(message)
                .totalCount(totalCount)
                .successCount(0)
                .failCount(totalCount)
                .build();
    }

    /**
     * 判断是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 获取成功率
     *
     * @return 成功率百分比
     */
    public Double getSuccessRate() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100;
    }

    /**
     * 获取导入摘要
     *
     * @return 导入摘要
     */
    public String getImportSummary() {
        if (totalCount == null) {
            return "导入失败";
        }
        return String.format("总计 %d 条记录，成功 %d 条，失败 %d 条，成功率 %.2f%%",
                totalCount, successCount, failCount, getSuccessRate());
    }
}