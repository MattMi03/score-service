package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.PfgjEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评分轨迹Mapper接口
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Mapper
public interface PfgjMapper {

    /**
     * 批量插入评分轨迹数据
     *
     * @param pfgjList 评分轨迹数据列表
     * @return 插入成功的记录数
     */
    int batchInsert(@Param("list") List<PfgjEntity> pfgjList);

    /**
     * 根据条件删除评分轨迹数据（用于覆盖导入）
     *
     * @param ksjhdm   考试计划代码
     * @param lshList  流水号列表
     * @param itemid   大题序号
     * @param tasktype 评次
     * @return 删除的记录数
     */
    int deleteByConditions(@Param("ksjhdm") String ksjhdm,
                           @Param("lshList") List<String> lshList,
                           @Param("itemid") Integer itemid,
                           @Param("tasktype") String tasktype);

    /**
     * 根据考试计划代码和流水号查询评分轨迹
     *
     * @param ksjhdm 考试计划代码
     * @param lsh    流水号
     * @return 评分轨迹列表
     */
    List<PfgjEntity> selectByKsjhdmAndLsh(@Param("ksjhdm") String ksjhdm,
                                          @Param("lsh") String lsh);

    /**
     * 根据考试计划代码、大题序号和评次查询评分轨迹
     *
     * @param ksjhdm   考试计划代码
     * @param itemid   大题序号
     * @param tasktype 评次
     * @return 评分轨迹列表
     */
    List<PfgjEntity> selectByKsjhdmAndItemidAndTasktype(@Param("ksjhdm") String ksjhdm,
                                                        @Param("itemid") Integer itemid,
                                                        @Param("tasktype") String tasktype);

    /**
     * 统计评分轨迹数量
     *
     * @param ksjhdm   考试计划代码
     * @param itemid   大题序号
     * @param tasktype 评次
     * @return 记录数量
     */
    int countByConditions(@Param("ksjhdm") String ksjhdm,
                          @Param("itemid") Integer itemid,
                          @Param("tasktype") String tasktype);

    /**
     * 根据阅卷序号和考试计划代码查询评分轨迹
     *
     * @param yjxh   阅卷序号
     * @param ksjhdm 考试计划代码
     * @return 评分轨迹列表
     */
    List<PfgjEntity> selectByYjxhAndKsjhdm(@Param("yjxh") String yjxh,
                                           @Param("ksjhdm") String ksjhdm);

    /**
     * 根据阅卷序号批量查询评分轨迹
     *
     * @param yjxhList 阅卷序号列表
     * @param ksjhdm   考试计划代码
     * @return 评分轨迹列表
     */
    List<PfgjEntity> selectByYjxhList(@Param("yjxhList") List<Long> yjxhList,
                                      @Param("ksjhdm") String ksjhdm);
}