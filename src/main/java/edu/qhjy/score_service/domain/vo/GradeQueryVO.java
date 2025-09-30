package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成绩查询响应VO
 * 用于前端展示学生基本信息和成绩数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩查询响应")
public class GradeQueryVO {

    private String xxdm; // [NEW] 新增此字段，用于权限过滤

    @Schema(description = "考籍号", example = "00001")
    private String ksh;

    @Schema(description = "考生姓名", example = "张三")
    private String xm;

    @Schema(description = "身份证件号", example = "430102200001010001")
    private String sfzjh;

    @Schema(description = "年级（入学年度）", example = "2023")
    private String grade;

    @Schema(description = "班级名称", example = "高三1班")
    private String bj;

    @Schema(description = "性别", example = "男")
    private String xb;

    @Schema(description = "民族", example = "汉族")
    private String mz;

    @Schema(description = "地市名称", example = "长沙市")
    private String szsmc;

    @Schema(description = "考区名称（区县）", example = "岳麓区")
    private String kqmc;

    @Schema(description = "学校名称", example = "长沙市第一中学")
    private String xxmc;

    @Schema(description = "成绩数据，格式：{\"科目名称1\": 等第码或合格码1, \"科目名称2\": 等第码或合格码2}")
    @Builder.Default
    private Map<String, String> scores = new LinkedHashMap<>();

    /**
     * 添加成绩
     *
     * @param subjectName 科目名称
     * @param score       分数
     */
    public void addScore(String subjectName, String score) {
        if (subjectName != null && score != null) {
            this.scores.put(subjectName, score);
        }
    }

    /**
     * 获取指定科目的成绩
     *
     * @param subjectName 科目名称
     * @return 分数，如果不存在则返回null
     */
    public String getScore(String subjectName) {
        return this.scores.get(subjectName);
    }

    /**
     * 移除指定科目的成绩
     *
     * @param subjectName 科目名称
     */
    public void removeScore(String subjectName) {
        this.scores.remove(subjectName);
    }

    /**
     * 清空所有成绩
     */
    public void clearScores() {
        this.scores.clear();
    }

    /**
     * 判断是否有成绩数据
     *
     * @return true表示有成绩数据
     */
    public boolean hasScores() {
        return scores != null && !scores.isEmpty();
    }

    /**
     * 获取成绩科目数量
     *
     * @return 科目数量
     */
    public int getSubjectCount() {
        return scores != null ? scores.size() : 0;
    }
}