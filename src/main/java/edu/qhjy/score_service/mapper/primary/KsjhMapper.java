package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.KsjhEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试计划Mapper
 */
@Mapper
public interface KsjhMapper {

    /**
     * 根据考试计划代码查询考试计划信息
     *
     * @param ksjhdm 考试计划代码
     * @return 考试计划信息
     */
    KsjhEntity selectByKsjhdm(@Param("ksjhdm") String ksjhdm);

    /**
     * 查询所有考试计划信息
     *
     * @return 考试计划列表
     */
    List<KsjhEntity> selectAll();
}