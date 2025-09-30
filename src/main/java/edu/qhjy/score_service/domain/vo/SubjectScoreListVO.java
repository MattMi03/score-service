package edu.qhjy.score_service.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 科目成绩列表响应VO
 */
@Data
public class SubjectScoreListVO {

    /**
     * 学生成绩详情列表
     */
    private List<StudentScoreDetailVO> scoreList;

    /**
     * 参加该考试计划下科目考试的学生总人数
     */
    private Integer totalStudentCount;

    /**
     * 考试计划代码
     */
    private String ksjhdm;

    /**
     * 科目名称
     */
    private String kmmc;
}