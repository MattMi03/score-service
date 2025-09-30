package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.InitializeExamStudentsDTO;
import edu.qhjy.score_service.domain.entity.KskmxxEntity;
import edu.qhjy.score_service.domain.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 成绩管理服务接口
 */
public interface ScoreService {

    /**
     * 查询所有模板（科目）列表
     */
    List<KskmxxEntity> listTemplates();

    /**
     * 查询模板（科目）详情
     */
    KskmxxEntity getTemplate(String ksjhdm, String kmdm);

    /**
     * 删除模板（科目）
     */
    void deleteTemplate(String ksjhdm, String kmdm);

    // ==================== 统计分析接口 ====================

    /**
     * 获取区域成绩统计分布（柱状图）
     *
     * @param subjectName  科目名称
     * @param examPlanCode 考试计划代码
     * @param areaType     区域类型（city/county/school/grade/class）
     * @param parentArea   上级区域筛选条件
     * @return 区域成绩统计数据
     */
    AreaScoreStatisticsVO getAreaScoreStatistics(String subjectName, String examPlanCode,
                                                 String areaType, String parentArea);

    /**
     * 获取科目成绩等级分布（饼图）
     *
     * @param subjectName  科目名称
     * @param examPlanCode 考试计划代码
     * @param areaFilter   区域筛选条件
     * @return 科目成绩等级分布数据
     */
    SubjectGradeDistributionVO getSubjectGradeDistribution(String subjectName, String examPlanCode,
                                                           String areaFilter);

    /**
     * 获取历史成绩趋势分析（折线图）
     *
     * @param subjectName 科目名称
     * @param areaFilter  区域筛选条件
     * @param startYear   开始年份
     * @param endYear     结束年份
     * @return 历史成绩趋势数据
     */
    ScoreTrendAnalysisVO getHistoricalTrends(String subjectName, String areaFilter,
                                             Integer startYear, Integer endYear);

    /**
     * 获取可用的考试计划列表
     *
     * @param subjectName 科目名称（可选）
     * @return 考试计划列表
     */
    List<String> getAvailableExamPlans(String subjectName);

    /**
     * 获取区域层级数据（增强版）
     * 用于前端级联下拉选择框，支持React组件
     *
     * @param parentArea 上级区域（可选）
     * @param areaType   区域类型（city/county/school/grade/class）
     * @return 区域层级数据（包含完整信息）
     */
    List<AreaHierarchyVO> getAreaHierarchyEnhanced(String parentArea, String areaType);

    /**
     * 获取可用的科目列表
     *
     * @return 科目列表
     */
    List<String> getAvailableSubjects();

    /**
     * 查询考试计划科目统计信息
     *
     * @param ksjhdm 考试计划代码（可选）
     * @return 考试计划科目统计信息列表
     */
    List<ExamPlanStatisticsVO> getExamPlanStatistics(String ksjhdm);

    /**
     * 根据考试计划代码和科目类型查询科目名称列表
     *
     * @param ksjhdm 考试计划代码
     * @param kmlx   科目类型（1表示考查性科目）
     * @return 科目名称列表
     */
    List<String> getSubjectsByKsjhdmAndKmlx(String ksjhdm, Integer kmlx);

    /**
     * 分页查询考查科目成绩数据
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<edu.qhjy.score_service.domain.vo.ExamScoreVO> getExamScoresWithPagination(
            edu.qhjy.score_service.domain.dto.ExamScoreQueryDTO query);

    /**
     * 导入考试科目考生（初始化接口）
     *
     * @param request 初始化请求参数
     * @return 初始化结果
     */
    InitializeResultVO initializeExamStudents(InitializeExamStudentsDTO request);

    /**
     * 生成Excel导入模板
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  地市名称（必传）
     * @param kqmc   考区名称（必传）
     * @param xxmc   学校名称（必传）
     * @return Excel文件字节数组
     */
    byte[] generateExcelTemplate(String ksjhdm, String kmmc, String szsmc, String kqmc, String xxmc);

    /**
     * 导入Excel成绩文件
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param file   Excel文件
     * @return 导入结果
     */
    ImportResultVO importExcelScores(String ksjhdm, String kmmc, MultipartFile file);
}