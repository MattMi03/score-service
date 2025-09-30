package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 市州调整数据DTO
 * 用于支持多市州批量等级调整
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "市州调整数据")
public class AdjustedSzsmcDTO {

    @Schema(description = "市州名称", example = "长沙市")
    @NotBlank(message = "市州名称不能为空")
    private String szsmc;

    @Schema(description = "调整后的等级分界线", example = "{\"A\": 85.5, \"B\": 75.0, \"C\": 65.0, \"D\": 55.0}")
    @NotNull(message = "调整后的等级分界线不能为空")
    private Map<String, BigDecimal> adjustedThresholds;

    /**
     * 验证调整等级的完整性
     * 确保包含A、B、C、D四个等级的分界线
     */
    public boolean validateAdjustmentLevel() {
        if (adjustedThresholds == null || adjustedThresholds.isEmpty()) {
            return false;
        }

        // 检查是否包含所有必需的等级
        String[] requiredGrades = {"A", "B", "C", "D"};
        for (String grade : requiredGrades) {
            if (!adjustedThresholds.containsKey(grade) || adjustedThresholds.get(grade) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取缺失的等级列表
     */
    public String getMissingGrades() {
        if (adjustedThresholds == null) {
            return "A, B, C, D";
        }

        StringBuilder missing = new StringBuilder();
        String[] requiredGrades = {"A", "B", "C", "D"};

        for (String grade : requiredGrades) {
            if (!adjustedThresholds.containsKey(grade) || adjustedThresholds.get(grade) == null) {
                if (!missing.isEmpty()) {
                    missing.append(", ");
                }
                missing.append(grade);
            }
        }

        return missing.toString();
    }
}