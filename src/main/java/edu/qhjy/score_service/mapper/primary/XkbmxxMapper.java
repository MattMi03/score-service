package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.XkbmxxEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学考报名信息Mapper (score数据库)
 */
@Mapper
public interface XkbmxxMapper {

    /**
     * 根据考生号查询报名信息
     */
    List<XkbmxxEntity> selectByKsh(@Param("ksh") String ksh);

    /**
     * 根据考试计划查询报名信息
     */
    List<XkbmxxEntity> selectByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 根据考试计划查询已缴费的报名信息
     */
    List<XkbmxxEntity> selectPaidByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 根据考试计划和科目名称联表查询报名信息
     */
    List<XkbmxxEntity> selectByKsjhdmAndKmmc(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);

    /**
     * 根据考试计划和科目名称联表查询已缴费的报名信息
     */
    List<XkbmxxEntity> selectPaidByKsjhdmAndKmmc(@Param("ksjhdm") String ksjhdm, @Param("kmmc") String kmmc);
}