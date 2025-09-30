package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.service.redis.SubjectCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科目缓存管理控制器
 * 提供科目缓存的查询和管理功能
 *
 * @author dadalv
 * @since 2025-09-05
 */
@Slf4j
@RestController
@RequestMapping("/api/subject-cache")
@RequiredArgsConstructor
@Tag(name = "科目缓存管理", description = "科目缓存的查询和管理接口")
public class SubjectCacheController {

    private final SubjectCacheService subjectCacheService;

    /**
     * 获取指定考试计划和科目类型的科目列表
     */
    @Operation(summary = "获取科目列表", description = "获取指定考试计划和科目类型的科目名称列表（带缓存）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/subjects")
    public Result<List<String>> getSubjects(
            @Parameter(description = "考试计划代码", required = true, example = "202507")
            @RequestParam("ksjhdm") String ksjhdm,
            @Parameter(description = "科目类型（0表示合格性科目，1表示考查性科目）", required = true, example = "0")
            @RequestParam("kmlx") Integer kmlx) {

        try {
            log.info("获取科目列表，考试计划代码：{}，科目类型：{}", ksjhdm, kmlx);
            List<String> subjects = subjectCacheService.getSubjectNamesByKsjhdmAndKmlx(ksjhdm, kmlx);
            log.info("获取科目列表成功，返回{}个科目", subjects.size());
            return Result.success(subjects);
        } catch (Exception e) {
            log.error("获取科目列表失败", e);
            return Result.error("获取科目列表失败：" + e.getMessage());
        }
    }

    /**
     * 清除指定考试计划的科目缓存
     */
    @Operation(summary = "清除科目缓存", description = "清除指定考试计划的科目缓存数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/cache/{ksjhdm}")
    public Result<String> evictSubjectCache(
            @Parameter(description = "考试计划代码", required = true, example = "202507")
            @PathVariable("ksjhdm") String ksjhdm) {

        try {
            log.info("清除科目缓存，考试计划代码：{}", ksjhdm);
            subjectCacheService.evictSubjectCache(ksjhdm);
            log.info("清除科目缓存成功");
            return Result.success("清除科目缓存成功");
        } catch (Exception e) {
            log.error("清除科目缓存失败", e);
            return Result.error("清除科目缓存失败：" + e.getMessage());
        }
    }

    /**
     * 清除所有科目缓存
     */
    @Operation(summary = "清除所有科目缓存", description = "清除所有科目缓存数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清除成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/cache/all")
    public Result<String> evictAllSubjectCache() {

        try {
            log.info("清除所有科目缓存");
            subjectCacheService.evictAllSubjectCache();
            log.info("清除所有科目缓存成功");
            return Result.success("清除所有科目缓存成功");
        } catch (Exception e) {
            log.error("清除所有科目缓存失败", e);
            return Result.error("清除所有科目缓存失败：" + e.getMessage());
        }
    }
}