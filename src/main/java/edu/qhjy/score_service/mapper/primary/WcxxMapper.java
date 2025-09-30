package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.WcxxEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 位次信息Mapper
 */
@Mapper
public interface WcxxMapper {

    /**
     * 插入位次信息
     */
    int insert(WcxxEntity entity);

    /**
     * 批量插入位次信息
     */
    int batchInsert(@Param("list") List<WcxxEntity> list);

    /**
     * 根据ID查询位次信息
     */
    WcxxEntity selectById(@Param("wcbs") Long wcbs);

    /**
     * 查询指定条件的位次信息
     */
    List<WcxxEntity> selectByCondition(
            @Param("ksjhmc") String ksjhmc,
            @Param("szsmc") String szsmc,
            @Param("kmmc") String kmmc);

    /**
     * 删除指定条件的位次信息
     */
    int deleteByCondition(
            @Param("ksjhdm") String ksjhdm,
            @Param("szsmc") String szsmc,
            @Param("kmmc") String kmmc);

    /**
     * 更新位次信息
     */
    int updateById(WcxxEntity entity);

    /**
     * 查询位次统计信息
     */
    List<java.util.Map<String, Object>> selectStatistics(
            @Param("ksjhmc") String ksjhmc,
            @Param("kmmc") String kmmc);

    /**
     * 检查是否存在等级赋分记录
     */
    boolean existsGradeAssignment(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 获取等级分布数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称（可选）
     * @return 等级分布信息列表
     */
    List<java.util.Map<String, Object>> getGradeDistribution(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 更新等级分界线
     *
     * @param ksjhdm          考试计划代码
     * @param kmmc            科目名称
     * @param szsmc           市州名称
     * @param gradeThresholds 等级分界线Map
     * @param operatorName    操作人姓名
     * @param operatorCode    操作人工作人员码
     * @return 更新的记录数
     */
    int updateGradeThresholds(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc,
            @Param("gradeThresholds") java.util.Map<String, java.math.BigDecimal> gradeThresholds,
            @Param("operatorName") String operatorName,
            @Param("operatorCode") String operatorCode);

    /**
     * 查询一分一段数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称（可选）
     * @return 一分一段数据列表
     */
    List<WcxxEntity> selectScoreSegmentData(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 批量插入一分一段数据
     *
     * @param list 一分一段数据列表
     * @return 插入的记录数
     */
    int batchInsertScoreSegments(@Param("list") List<WcxxEntity> list);

    /**
     * 删除一分一段数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称（可选）
     * @return 删除的记录数
     */
    int deleteScoreSegmentData(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 查询等级赋分统计信息（从WCXX表中DJZDF=0的记录）
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称（可选）
     * @return 等级分布统计列表
     */
    List<java.util.Map<String, Object>> selectGradeAssignmentStats(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);

    /**
     * 更新等级统计数据（DJZDF=0的记录）
     *
     * @param ksjhdm               考试计划代码
     * @param kmmc                 科目名称
     * @param szsmc                市州名称
     * @param grade                等级代码
     * @param threshold            分界线
     * @param percentage           百分比
     * @param count                人数
     * @param cumulativePercentage 累计百分比
     * @param cumulativeCount      累计人数
     * @param operatorName         操作人姓名
     * @param operatorCode         操作人工作人员码
     * @return 更新的记录数
     */
    int updateGradeStatistics(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc,
            @Param("grade") String grade,
            @Param("threshold") java.math.BigDecimal threshold,
            @Param("percentage") int percentage,
            @Param("count") int count,
            @Param("cumulativePercentage") java.math.BigDecimal cumulativePercentage,
            @Param("cumulativeCount") int cumulativeCount,
            @Param("operatorName") String operatorName,
            @Param("operatorCode") String operatorCode);

    /**
     * 更新WCXX表中一分一段数据的DJM字段
     */
    int updateScoreSegmentGrades(@Param("ksjhdm") String ksjhdm,
                                 @Param("kmmc") String kmmc,
                                 @Param("szsmc") String szsmc,
                                 @Param("gradeAThreshold") java.math.BigDecimal gradeAThreshold,
                                 @Param("gradeBThreshold") java.math.BigDecimal gradeBThreshold,
                                 @Param("gradeCThreshold") java.math.BigDecimal gradeCThreshold,
                                 @Param("gradeDThreshold") java.math.BigDecimal gradeDThreshold);

    /**
     * 获取等级分界线
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 等级分界线列表，每个Map包含key(等级)和value(分数线)
     */
    List<java.util.Map<String, Object>> getGradeThresholds(
            @Param("ksjhdm") String ksjhdm,
            @Param("kmmc") String kmmc,
            @Param("szsmc") String szsmc);
}