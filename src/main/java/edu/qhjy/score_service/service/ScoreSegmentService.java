package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.GradeAdjustmentRequestDTO;
import edu.qhjy.score_service.domain.dto.ScoreSegmentDTO;
import edu.qhjy.score_service.domain.dto.ScoreSegmentQueryDTO;
import edu.qhjy.score_service.domain.vo.*;

import java.util.List;

/**
 * 一分一段表服务接口
 * 提供一分一段表的核心功能
 */
public interface ScoreSegmentService {

    /**
     * 获取一分一段表总览数据
     *
     * @param queryDTO 查询参数
     * @return 总览数据
     */
//        ScoreSegmentOverviewVO getScoreSegmentOverview(ScoreSegmentQueryDTO queryDTO);

    /**
     * 预览等级调整结果
     *
     * @param requestDTO 调整请求
     * @return 调整预览结果
     */
    GradeAdjustmentResultVO previewGradeAdjustment(GradeAdjustmentRequestDTO requestDTO);

    /**
     * 确认并保存等级调整
     *
     * @param requestDTO 调整请求
     * @return 调整确认结果（包含等级调整结果和一分一段数据变化）
     */
    GradeAdjustmentConfirmResultVO confirmGradeAdjustment(GradeAdjustmentRequestDTO requestDTO);

    /**
     * 批量确认并保存多市州等级调整
     *
     * @param requestDTO 批量调整请求
     * @return 批量调整确认结果
     */
    BatchGradeAdjustmentResultVO batchConfirmGradeAdjustment(GradeAdjustmentRequestDTO requestDTO);

    /**
     * 获取历史考试计划列表
     *
     * @return 历史考试计划列表
     */
    List<ExamPlanSubjectStatisticsVO> getHistoricalExamPlans();

    /**
     * 预计算一分一段表数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 是否预计算成功
     */
//        Boolean preCalculateScoreSegment(String ksjhdm, String kmmc);

    /**
     * 清除一分一段表缓存
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     */
    void clearScoreSegmentCache(String ksjhdm, String kmmc);

    /**
     * 获取一分一段表计算状态
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 计算状态
     */
    String getCalculationStatus(String ksjhdm, String kmmc);

    /**
     * 导出一分一段表数据到Excel
     *
     * @param queryDTO 查询参数
     * @return Excel文件字节数组
     */
    byte[] exportScoreSegmentToExcel(ScoreSegmentQueryDTO queryDTO);

    /**
     * 批量预计算多个考试计划的一分一段表
     *
     * @param examPlans 考试计划列表
     * @return 预计算任务ID
     */
    String batchPreCalculate(List<String> examPlans);

    /**
     * 获取批量计算进度
     *
     * @param taskId 任务ID
     * @return 进度信息
     */
    GradeAssignmentProgressVO getBatchCalculationProgress(String taskId);

    /**
     * 保存一分一段数据到数据库
     *
     * @param ksjhdm       考试计划代码
     * @param kmmc         科目名称
     * @param szsmc        市州名称（可选）
     * @param operatorName 操作人姓名
     * @param operatorCode 操作人工作人员码
     * @return 是否保存成功
     */
    Boolean saveScoreSegmentData(String ksjhdm, String kmmc, String szsmc,
                                 String operatorName, String operatorCode);

    /**
     * 从数据库查询一分一段数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称（可选）
     * @return 一分一段数据列表
     */
    List<ScoreSegmentDTO> getScoreSegmentDataFromDB(String ksjhdm, String kmmc, String szsmc);

    /**
     * 删除一分一段数据
     * 支持灵活删除模式：
     * 1. 当只传入kmmc时，删除该科目下的所有一分一段数据
     * 2. 当只传入szsmc时，删除该市州下的所有一分一段数据
     * 3. 当同时传入kmmc和szsmc时，删除指定科目和市州的一分一段数据
     *
     * @param ksjhdm 考试计划代码（必填）
     * @param kmmc   科目名称（与szsmc至少传入一个）
     * @param szsmc  市州名称（与kmmc至少传入一个）
     * @return 是否删除成功
     */
    Boolean deleteScoreSegmentData(String ksjhdm, String kmmc, String szsmc);

    /**
     * 手动将WCXX表的DJM同步到KSCJ表的CJDJM字段
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 同步结果
     */
    GradeSyncResultVO syncStudentGradesFromWcxx(String ksjhdm, String kmmc, String szsmc);

    /**
     * 预览等级调整后的一分一段数据变化
     *
     * @param requestDTO 等级调整请求
     * @return 一分一段数据变化预览结果
     */
    ScoreSegmentChangePreviewVO previewScoreSegmentChanges(GradeAdjustmentRequestDTO requestDTO);
}