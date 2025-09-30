package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.YjxhEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 阅卷序号Mapper接口
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Mapper
public interface YjxhMapper {

    /**
     * 根据阅卷序号和考试计划代码查询考生信息
     *
     * @param yjxh   阅卷序号
     * @param ksjhdm 考试计划代码
     * @return 考生信息
     */
    YjxhEntity selectByYjxhAndKsjhdm(@Param("yjxh") String yjxh, @Param("ksjhdm") String ksjhdm);

    /**
     * 批量查询阅卷序号对应的考生信息
     *
     * @param yjxhList 阅卷序号列表
     * @param ksjhdm   考试计划代码
     * @return 考生信息列表
     */
    List<YjxhEntity> selectBatchByYjxhAndKsjhdm(@Param("yjxhList") List<String> yjxhList,
                                                @Param("ksjhdm") String ksjhdm);

    /**
     * 根据考试计划代码查询所有阅卷序号信息
     *
     * @param ksjhdm 考试计划代码
     * @return 阅卷序号信息列表
     */
    List<YjxhEntity> selectByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 验证阅卷序号、考试计划代码和考生姓名的匹配关系
     *
     * @param yjxh   阅卷序号
     * @param ksjhdm 考试计划代码
     * @param ksxm   考生姓名
     * @return 匹配的考生信息，如果不匹配返回null
     */
    YjxhEntity validateYjxhKsxmMatch(@Param("yjxh") String yjxh, @Param("ksjhdm") String ksjhdm,
                                     @Param("ksxm") String ksxm);

    /**
     * 根据考试计划代码和科目名称查询阅卷序号信息
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 阅卷序号信息列表
     */
    List<YjxhEntity> selectByKsjhdmAndKmmc(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);

    /**
     * 根据阅卷序号列表、考试计划代码和科目名称查询阅卷序号信息（分批查询优化）
     *
     * @param yjxhList 阅卷序号列表
     * @param ksjhdm   考试计划代码
     * @param kmmc     科目名称
     * @return 阅卷序号信息列表
     */
    List<YjxhEntity> selectByYjxhList(@Param("yjxhList") List<String> yjxhList,
                                      @Param("ksjhdm") String ksjhdm,
                                      @Param("kmmc") String kmmc);

    /**
     * 根据考试计划代码和考生号、科目名称查询阅卷序号
     *
     * @param ksjhdm 考试计划代码
     * @param ksh    考生号
     * @param kmmc   科目名称
     * @return 阅卷序号信息
     */
    YjxhEntity selectByKsjhdmAndKshAndKmmc(@Param("ksjhdm") String ksjhdm,
                                           @Param("ksh") String ksh,
                                           @Param("kmmc") String kmmc);

    /**
     * 根据考试计划代码查询考试计划名称
     *
     * @param ksjhdm 考试计划代码
     * @return 考试计划名称
     */
    String selectKsjhmcByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 根据考试计划代码查询考试计划和科目的统计信息
     *
     * @param ksjhdm 考试计划代码（可选）
     * @return 统计信息列表
     */
    List<java.util.Map<String, Object>> selectExamPlanSubjectStatistics(@Param("ksjhdm") String ksjhdm);
}