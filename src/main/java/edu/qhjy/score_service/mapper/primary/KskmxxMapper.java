package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.KskmxxEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试科目信息Mapper
 */
@Mapper
public interface KskmxxMapper {

    /**
     * 查询所有考试科目信息
     */
    List<KskmxxEntity> selectAll();

    /**
     * 根据考试计划代码查询科目列表
     */
    List<KskmxxEntity> selectByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 根据科目名称查询
     */
    List<KskmxxEntity> selectByKmmc(@Param("kmmc") String kmmc);

    /**
     * 根据考试计划和科目名称查询
     */
    KskmxxEntity selectByKsjhdmAndKmmc(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);

    /**
     * 根据科目代码查询
     */
    List<KskmxxEntity> selectByKmdm(@Param("kmdm") String kmdm);

    /**
     * 根据考试计划代码和科目代码查询
     */
    KskmxxEntity selectByKsjhdmAndKmdm(@Param("ksjhdm") String ksjhdm, @Param("kmdm") String kmdm);

    /**
     * 插入考试科目信息
     */
    int insert(KskmxxEntity entity);

    /**
     * 更新考试科目信息
     */
    int updateByKsjhdmAndKmdm(KskmxxEntity entity);

    /**
     * 根据复合主键删除考试科目信息
     */
    int deleteByKsjhdmAndKmdm(@Param("ksjhdm") String ksjhdm, @Param("kmdm") String kmdm);

    /**
     * 根据考试计划代码删除所有科目
     */
    int deleteByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 根据考试计划代码和科目类型查询科目名称列表
     *
     * @param ksjhdm 考试计划代码
     * @param kmlx   科目类型（1表示考查性科目）
     * @return 科目名称列表
     */
    List<String> selectKmmcByKsjhdmAndKmlx(@Param("ksjhdm") String ksjhdm, @Param("kmlx") Integer kmlx);

    /**
     * 根据考试计划代码查询所有科目类型的科目名称列表（kmlx=0和kmlx=1）
     */
    List<String> selectKmmcByKsjhdmAllTypes(@Param("ksjhdm") String ksjhdm);
}