package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.dto.GradeBookQueryDTO;
import edu.qhjy.score_service.domain.dto.GradeQueryDTO;
import edu.qhjy.score_service.domain.dto.StudentDataQueryDTO;
import edu.qhjy.score_service.domain.entity.KscjEntity;
import edu.qhjy.score_service.domain.vo.ExamScoreVO;
import edu.qhjy.score_service.domain.vo.GradeBookVO;
import edu.qhjy.score_service.domain.vo.GradeQueryVO;
import edu.qhjy.score_service.domain.vo.StudentDataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 考试汇总成绩Mapper
 */
@SuppressWarnings("MybatisXMapperMethodInspection")
@Mapper
public interface KscjMapper {

    int batchUpsertScores(List<KscjEntity> list);

    /**
     * 查询所有成绩记录
     */
    List<KscjEntity> selectAll();

    /**
     * 根据ID查询成绩
     */
    KscjEntity selectById(@Param("kscjbs") Long kscjbs);

    /**
     * 根据考生号和科目名称查询成绩
     */
    KscjEntity selectByKshAndKmmc(@Param("ksh") String ksh, @Param("kmmc") String kmmc);

    /**
     * 根据考生号查询所有科目成绩
     */
    List<KscjEntity> selectByKsh(@Param("ksh") String ksh);

    /**
     * 根据考生号查询所有科目成绩映射（科目名称->成绩合格评定）
     */
    List<java.util.Map<String, String>> selectScoresByKsh(@Param("ksh") String ksh);

    /**
     * 分页查询考查科目成绩数据
     * 支持按市、考区、学校级联筛选
     */
    List<ExamScoreVO> selectExamScoresWithPagination(
            @Param("query") edu.qhjy.score_service.domain.dto.ExamScoreQueryDTO query);

    /**
     * 统计考查科目成绩数据总数
     * 用于分页计算
     */
    Long countExamScores(@Param("query") edu.qhjy.score_service.domain.dto.ExamScoreQueryDTO query);

    /**
     * 根据科目名称查询所有成绩
     */
    List<KscjEntity> selectByKmmc(@Param("kmmc") String kmmc);

    /**
     * 根据考试计划和科目名称查询所有成绩
     */
    List<KscjEntity> selectByKsjhdmAndKmmc(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);

    /**
     * 根据班级和科目名称查询成绩
     */
    List<KscjEntity> selectByBjmcAndKmmc(@Param("bjmc") String bjmc, @Param("kmmc") String kmmc);

    /**
     * 插入成绩
     */
    int insert(KscjEntity entity);

    /**
     * 批量插入成绩
     */
    int batchInsert(@Param("list") List<KscjEntity> list);

    /**
     * 批量插入成绩（优化版本，只插入必要字段）
     */
    int batchInsertOptimized(@Param("list") List<KscjEntity> list);

    /**
     * 根据考生号列表、考试计划代码和科目名称批量删除成绩记录（用于DBF导入覆盖模式）
     *
     * @param kshList 考生号列表
     * @param ksjhdm  考试计划代码
     * @param kmmc    科目名称
     * @return 删除的记录数
     */
    int deleteByKshListAndKsjhdmAndKmmc(@Param("kshList") List<String> kshList,
                                        @Param("ksjhdm") String ksjhdm,
                                        @Param("kmmc") String kmmc);

    /**
     * 更新成绩
     */
    int updateById(KscjEntity entity);

    /**
     * 删除成绩
     */
    int deleteById(@Param("kscjbs") Long kscjbs);

    /**
     * 分页查询学生数据
     * 支持多条件筛选和分页
     */
    List<StudentDataVO> selectStudentDataWithPagination(@Param("query") StudentDataQueryDTO query);

    /**
     * 统计学生数据总数
     * 用于分页计算
     */
    Long countStudentData(@Param("query") StudentDataQueryDTO query);

    /**
     * 分页查询成绩数据（支持级联查询）
     * 支持多条件筛选和分页，返回学生基本信息和成绩数据
     */
    List<GradeQueryVO> selectGradeDataWithPagination(@Param("query") GradeQueryDTO query);

    /**
     * 分页查询成绩数据（使用ResultHandler处理）
     * 直接将科目成绩映射到scores Map中
     */
    void selectGradeDataWithResultHandler(@Param("query") GradeQueryDTO query,
                                          org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>> resultHandler);

    /**
     * 统计成绩数据总数（支持级联查询）
     * 用于分页计算
     */
    Long countGradeData(@Param("query") GradeQueryDTO query);

    /**
     * 根据考生号和科目名称删除成绩
     */
    int deleteByKshAndKmmc(@Param("ksh") String ksh, @Param("kmmc") String kmmc);

    /**
     * 根据科目名称删除所有成绩
     */
    int deleteByKmmc(@Param("kmmc") String kmmc);

    /**
     * 统计科目成绩信息
     */
    java.util.Map<String, Object> selectScoreStatistics(@Param("kmmc") String kmmc);

    /**
     * 查询科目成绩分布
     */
    List<java.util.Map<String, Object>> selectScoreDistribution(@Param("kmmc") String kmmc);

    /**
     * 根据考试计划和科目查询学生基础信息（用于模板预填充）
     */
    List<StudentDataVO> selectStudentInfoForTemplate(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);

    // ==================== 新增统计分析方法 ====================

    /**
     * 查询区域成绩统计分布（柱状图数据）
     *
     * @param ksjhdm    考试计划代码
     * @param kmmc      科目名称
     * @param areaLevel 区域级别(city/county/school/class)
     * @param areaCode  区域代码（可选，用于筛选特定区域）
     */
    List<java.util.Map<String, Object>> selectAreaScoreStatistics(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("areaLevel") String areaLevel,
            @Param("areaCode") String areaCode);

    /**
     * 查询科目成绩等级分布（饼图数据）
     *
     * @param ksjhdm    考试计划代码
     * @param kmmc      科目名称
     * @param areaLevel 区域级别
     * @param areaCode  区域代码
     */
    List<java.util.Map<String, Object>> selectSubjectGradeDistribution(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("areaLevel") String areaLevel,
            @Param("areaCode") String areaCode);

    /**
     * 查询历史成绩趋势数据（折线图数据）
     *
     * @param kmmc      科目名称
     * @param areaLevel 区域级别
     * @param areaCode  区域代码
     * @param startYear 开始年份
     * @param endYear   结束年份
     */
    List<java.util.Map<String, Object>> selectHistoricalTrends(
            @Param("kmmc") String kmmc,
            @Param("areaLevel") String areaLevel,
            @Param("areaCode") String areaCode,
            @Param("startYear") String startYear,
            @Param("endYear") String endYear);

    /**
     * 查询可用的考试计划列表（用于趋势分析）
     */
    List<String> selectAvailableExamPlans(@Param("kmmc") String kmmc);

    /**
     * 查询区域层级数据（用于下拉选择）
     *
     * @param areaLevel  区域级别
     * @param parentName 父级区域名称（可选）
     */
    List<java.util.Map<String, Object>> selectAreaHierarchy(
            @Param("areaLevel") String areaLevel,
            @Param("parentName") String parentName);

    /**
     * 查询可用的科目列表
     */
    List<String> selectAvailableSubjects();

    /**
     * 查询考试计划科目统计信息
     *
     * @param ksjhdm 考试计划代码（可选）
     * @return 考试计划科目统计信息列表
     */
    List<java.util.Map<String, Object>> selectExamPlanSubjectStatistics(@Param("ksjhdm") String ksjhdm);

    // ==================== 等级划分相关方法 ====================

    /**
     * 查询指定条件下的学生成绩排名数据（用于等级划分）
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 按成绩降序排列的学生数据
     */
    List<edu.qhjy.score_service.domain.dto.StudentScoreRankDTO> selectStudentScoreRanks(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 查询需要进行等级划分的市州列表
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 市州名称列表
     */
    List<String> selectCitiesForGradeAssignment(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc);

    /**
     * 批量更新学生等级信息
     *
     * @param ksjhdm          考试计划代码
     * @param kmmc            科目名称
     * @param szsmc           市州名称
     * @param gradeAThreshold A等级分数线
     * @param gradeBThreshold B等级分数线
     * @param gradeCThreshold C等级分数线
     * @param gradeDThreshold D等级分数线
     * @param operatorName    操作人姓名
     * @param operatorCode    操作人工作人员码
     * @return 更新的记录数
     */
    int batchUpdateGrades(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc,
            @Param("gradeAThreshold") BigDecimal gradeAThreshold,
            @Param("gradeBThreshold") BigDecimal gradeBThreshold,
            @Param("gradeCThreshold") BigDecimal gradeCThreshold,
            @Param("gradeDThreshold") BigDecimal gradeDThreshold,
            @Param("operatorName") String operatorName,
            @Param("operatorCode") String operatorCode);

    /**
     * 查询等级划分统计信息
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 等级分布统计列表
     */
    List<java.util.Map<String, Object>> selectGradeDistributionStats(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 清除学生等级信息（将cjdjm设为null）
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称（可选）
     * @return 更新的记录数
     */
    int clearStudentGrades(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    // ==================== 一分一段表相关方法 ====================

    /**
     * 获取基础统计数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 基础统计信息
     */
    java.util.Map<String, Object> getBasicStatistics(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc);

    /**
     * 获取市州统计数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 市州统计信息列表
     */
    List<java.util.Map<String, Object>> getCityStatistics(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc);

    /**
     * 获取考试计划下的市州列表
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 市州名称列表
     */
    List<String> getCitiesByExamPlan(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc);

    /**
     * 获取学生成绩排名数据（支持多市州）
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param cities 市州名称列表
     * @return 学生成绩排名数据
     */
    List<edu.qhjy.score_service.domain.dto.StudentScoreRankDTO> getStudentScoreRanks(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("cities") List<String> cities);

    /**
     * 获取历史考试计划（包含成绩数据）
     *
     * @return 历史考试计划统计信息
     */
    List<edu.qhjy.score_service.domain.vo.ExamPlanSubjectStatisticsVO> getHistoricalExamPlansWithScores();

    /**
     * 获取指定考试计划和科目的分数范围
     */
    java.util.Map<String, Object> getScoreRange(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("cities") List<String> cities);

    /**
     * 分页查询成绩等第册数据
     * 以学校为单位查询学生基本信息和成绩数据
     */
    List<GradeBookVO.StudentGradeData> selectGradeBookWithPagination(@Param("query") GradeBookQueryDTO query);

    /**
     * 统计成绩等第册数据总数
     * 用于分页计算
     */
    Long countGradeBookData(@Param("query") GradeBookQueryDTO query);

    /**
     * 查询学校信息（用于成绩册查询）
     */
    java.util.Map<String, String> selectSchoolInfo(@Param("query") GradeBookQueryDTO query);

    /**
     * 保存省外转入成绩记录
     *
     * @param entity 成绩实体
     * @return 影响行数
     */
    int insertOutOfProvinceScore(KscjEntity entity);

    /**
     * 批量保存省外转入成绩记录
     *
     * @param list 成绩实体列表
     * @return 影响行数
     */
    int batchInsertOutOfProvinceScores(@Param("list") List<KscjEntity> list);

    /**
     * 删除省外转入成绩记录（根据考生号和科目名称）
     *
     * @param ksh  考生号
     * @param kmmc 科目名称
     * @return 影响行数
     */
    int deleteOutOfProvinceScore(@Param("ksh") String ksh, @Param("kmmc") String kmmc);

    /**
     * 删除省外转入成绩记录（根据考生号删除所有科目）
     *
     * @param ksh 考生号
     * @return 影响行数
     */
    int deleteAllOutOfProvinceScoresByKsh(@Param("ksh") String ksh);

    /**
     * 修改省外转入成绩记录
     *
     * @param entity 成绩实体
     * @return 影响行数
     */
    int updateOutOfProvinceScore(KscjEntity entity);

    /**
     * 检查学生是否已存在成绩记录
     *
     * @param ksh    考生号
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 存在的记录数
     */
    int countExistingScore(@Param("ksh") String ksh,
                           @Param("ksjhdm") String ksjhdm,
                           @Param("kmmc") String kmmc);

    /**
     * 根据条件查询报名学生信息（用于初始化）
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  所在市名称
     * @param kqmc   考区名称
     * @param xxmc   学校名称（可选）
     * @return 学生报名信息列表（包含YJXH字段）
     */
    List<Map<String, Object>> selectStudentsForInitialize(@Param("ksjhdm") String ksjhdm,
                                                          @Param("kmmc") String kmmc,
                                                          @Param("szsmc") String szsmc,
                                                          @Param("kqmc") String kqmc,
                                                          @Param("xxmc") String xxmc);

    /**
     * 查询已存在的考籍号列表
     *
     * @param ksjhdm  考试计划代码
     * @param kmmc    科目名称
     * @param kshList 考籍号列表
     * @return 已存在的考籍号列表
     */
    List<String> selectExistingKsh(@Param("ksjhdm") String ksjhdm,
                                   @Param("kmmc") String kmmc,
                                   @Param("kshList") List<String> kshList);

    /**
     * 统计指定考试计划和科目的现有成绩记录数
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 现有记录数
     */
    int countExistingRecords(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);

    /**
     * 统计现有记录数（支持地区参数筛选）
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  所在市名称
     * @param kqmc   考区名称
     * @param xxmc   学校名称
     * @return 现有记录数
     */
    int countExistingRecordsWithArea(@Param("ksjhdm") String ksjhdm,
                                     @Param("kmmc") String kmmc,
                                     @Param("szsmc") String szsmc,
                                     @Param("kqmc") String kqmc,
                                     @Param("xxmc") String xxmc);

    /**
     * 批量更新成绩
     *
     * @param scoreUpdates 成绩更新列表
     * @return 更新的记录数
     */
    int batchUpdateScores(
            @Param("scoreUpdates") List<edu.qhjy.score_service.domain.dto.ScoreUpdateDTO> scoreUpdates);

    // ==================== DM8物化视图优化查询 ====================

    /**
     * DM8物化视图查询：从物化视图查询成绩数据（使用ResultHandler处理）
     *
     * @param ksjhdm        考试计划代码
     * @param kmlx          科目类型
     * @param szsmc         地市名称
     * @param kqmc          考区名称
     * @param xxmc          学校名称
     * @param ksh           考生号
     * @param grade         年级
     * @param bjmc          班级名称
     * @param offset        偏移量
     * @param limit         限制数量
     * @param resultHandler 结果处理器
     */
    void selectGradeDataFromMV(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmlx") Integer kmlx,
            @Param("szsmc") String szsmc,
            @Param("kqmc") String kqmc,
            @Param("xxmc") String xxmc,
            @Param("ksh") String ksh,
            @Param("grade") String grade,
            @Param("bjmc") String bjmc,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit,
            org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>> resultHandler);

    /**
     * DM8物化视图统计查询：从物化视图统计记录数
     *
     * @param ksjhdm 考试计划代码
     * @param kmlx   科目类型
     * @param szsmc  地市名称
     * @param kqmc   考区名称
     * @param xxmc   学校名称
     * @param ksh    考生号
     * @param grade  年级
     * @param bjmc   班级名称
     * @return 记录总数
     */
    Long countFromMaterializedView(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmlx") Integer kmlx,
            @Param("szsmc") String szsmc,
            @Param("kqmc") String kqmc,
            @Param("xxmc") String xxmc,
            @Param("ksh") String ksh,
            @Param("grade") String grade,
            @Param("bjmc") String bjmc);

    void clearTempImportTable();

    int batchInsertIntoTempTable(List<KscjEntity> list);

    int mergeFromTempTable();

}