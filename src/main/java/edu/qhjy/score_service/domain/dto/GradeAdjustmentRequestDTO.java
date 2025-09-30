package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 等级调整请求DTO
 * 用于手动调整等级分界线
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "等级调整请求")
public class GradeAdjustmentRequestDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码", example = "2024001", implementation = String.class, pattern = "^[0-9]{4}[0-9]{3}$", minLength = 7, maxLength = 10)
    private String ksjhdm;

    @NotBlank(message = "科目名称不能为空")
    @Schema(description = "科目名称", example = "语文", allowableValues = {"语文", "数学", "英语", "物理", "化学",
            "生物", "政治", "历史", "地理"})
    private String kmmc;

    @Schema(description = "市州调整数据列表")
    @NotNull(message = "市州调整数据不能为空")
    @Valid
    private List<AdjustedSzsmcDTO> adjustedszsmcs;

    @Schema(description = "操作人姓名，用于审计记录", example = "张三", maxLength = 20)
    private String operatorName;

    @Schema(description = "操作人工作人员码，用于权限验证和审计", example = "OP001", pattern = "^[A-Z]{2}[0-9]{3}$", maxLength = 10)
    private String operatorCode;

    @Schema(description = "调整原因说明，便于后续审计和追溯", example = "根据考试难度和专家评议结果调整等级分界线", maxLength = 200)
    private String adjustmentReason;

    /**
     * 验证所有市州调整数据的完整性
     * 确保每个市州都包含A、B、C、D四个等级的分界线
     */
    public boolean validateAdjustmentLevel() {
        if (adjustedszsmcs == null || adjustedszsmcs.isEmpty()) {
            return false;
        }

        // 检查每个市州的调整数据
        for (AdjustedSzsmcDTO cityData : adjustedszsmcs) {
            if (!cityData.validateAdjustmentLevel()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取第一个市州的调整数据（兼容性方法）
     */
    public AdjustedSzsmcDTO getFirstCityData() {
        if (adjustedszsmcs != null && !adjustedszsmcs.isEmpty()) {
            return adjustedszsmcs.get(0);
        }
        return null;
    }
}