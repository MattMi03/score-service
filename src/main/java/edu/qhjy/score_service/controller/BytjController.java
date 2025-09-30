package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.entity.BytjEntity;
import edu.qhjy.score_service.service.BytjService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 毕业条件设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/graduation/bytj")
@RequiredArgsConstructor
@Validated
@Tag(name = "毕业条件设置", description = "毕业条件设置管理相关接口")
public class BytjController {

    private final BytjService bytjService;

    /**
     * 查询所有毕业条件设置
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有毕业条件设置", description = "获取所有毕业条件设置记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<BytjEntity>> selectAll() {
        try {
            log.info("开始查询所有毕业条件设置");
            List<BytjEntity> result = bytjService.selectAll();
            log.info("查询所有毕业条件设置成功，返回{}条记录", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询所有毕业条件设置失败", e);
            return Result.error("查询毕业条件设置失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询毕业条件设置
     */
    @GetMapping("/{bytjbs}")
    @Operation(summary = "根据ID查询毕业条件设置", description = "根据毕业条件设置标识查询详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<BytjEntity> selectById(
            @Parameter(description = "毕业条件设置标识", required = true) @PathVariable("bytjbs") @NotNull Long bytjbs) {
        try {
            log.info("开始根据ID查询毕业条件设置，ID：{}", bytjbs);
            BytjEntity result = bytjService.selectById(bytjbs);
            if (result != null) {
                log.info("根据ID查询毕业条件设置成功，ID：{}", bytjbs);
                return Result.success(result);
            } else {
                log.warn("未找到ID为{}的毕业条件设置", bytjbs);
                return Result.error("未找到指定的毕业条件设置");
            }
        } catch (Exception e) {
            log.error("根据ID查询毕业条件设置失败，ID：{}", bytjbs, e);
            return Result.error("查询毕业条件设置失败：" + e.getMessage());
        }
    }

    /**
     * 新增毕业条件设置
     */
    @PostMapping
    @Operation(summary = "新增毕业条件设置", description = "创建新的毕业条件设置记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功", content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> insert(
            @Parameter(description = "毕业条件设置实体", required = true) @RequestBody @Valid BytjEntity bytjEntity) {
        try {
            log.info("开始新增毕业条件设置：{}", bytjEntity);
            int result = bytjService.insert(bytjEntity);
            log.info("新增毕业条件设置成功，影响行数：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("新增毕业条件设置失败", e);
            return Result.error("新增毕业条件设置失败：" + e.getMessage());
        }
    }

    /**
     * 更新毕业条件设置
     */
    @PutMapping("/{bytjbs}")
    @Operation(summary = "更新毕业条件设置", description = "根据ID更新毕业条件设置信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功", content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> updateById(
            @Parameter(description = "毕业条件设置标识", required = true) @PathVariable("bytjbs") @NotNull Long bytjbs,
            @Parameter(description = "毕业条件设置实体", required = true) @RequestBody @Valid BytjEntity bytjEntity) {
        try {
            // 确保路径参数和请求体中的ID一致
            bytjEntity.setBytjbs(bytjbs);
            log.info("开始更新毕业条件设置，ID：{}，数据：{}", bytjbs, bytjEntity);
            int result = bytjService.updateById(bytjEntity);
            log.info("更新毕业条件设置成功，影响行数：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新毕业条件设置失败，ID：{}", bytjbs, e);
            return Result.error("更新毕业条件设置失败：" + e.getMessage());
        }
    }

    /**
     * 删除毕业条件设置
     */
    @DeleteMapping("/{bytjbs}")
    @Operation(summary = "删除毕业条件设置", description = "根据ID删除毕业条件设置记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功", content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> deleteById(
            @Parameter(description = "毕业条件设置标识", required = true) @PathVariable("bytjbs") @NotNull Long bytjbs) {
        try {
            log.info("开始删除毕业条件设置，ID：{}", bytjbs);
            int result = bytjService.deleteById(bytjbs);
            log.info("删除毕业条件设置成功，影响行数：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除毕业条件设置失败，ID：{}", bytjbs, e);
            return Result.error("删除毕业条件设置失败：" + e.getMessage());
        }
    }
}