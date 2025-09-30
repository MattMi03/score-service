package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.KmxxEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 科目信息Mapper
 */
@Mapper
public interface KmxxMapper {

    /**
     * 查询所有科目信息
     *
     * @return 科目信息列表
     */
    List<KmxxEntity> selectAll();

    /**
     * 根据科目名称查询科目信息
     *
     * @param kmmc 科目名称
     * @return 科目信息
     */
    KmxxEntity selectByKmmc(@Param("kmmc") String kmmc);

    /**
     * 根据科目代码查询科目信息
     *
     * @param kmdm 科目代码
     * @return 科目信息
     */
    KmxxEntity selectByKmdm(@Param("kmdm") String kmdm);

    /**
     * 查询所有科目名称列表（用于考籍信息查询接口）
     *
     * @return 科目名称列表
     */
    List<String> selectAllKmmc();
}