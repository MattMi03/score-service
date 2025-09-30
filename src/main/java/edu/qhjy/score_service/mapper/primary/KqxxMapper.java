package edu.qhjy.score_service.mapper.primary;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 考区信息Mapper接口
 */
@Mapper
public interface KqxxMapper {

    /**
     * 根据所属行政区划名称查询所属行政区划代码
     *
     * @param ssxzqhmc 所属行政区划名称
     * @return 所属行政区划代码
     */
    String selectSsxzqhdmByName(@Param("ssxzqhmc") String ssxzqhmc);

    /**
     * 根据考区名称查询考区代码
     *
     * @param kqmc 考区名称
     * @return 考区代码
     */
    String selectKqdmByName(@Param("kqmc") String kqmc);

    /**
     * 根据所属行政区划代码查询所属行政区划名称
     *
     * @param ssxzqhdm 所属行政区划代码
     * @return 所属行政区划名称
     */
    String selectSsxzqhmcByCode(@Param("ssxzqhdm") String ssxzqhdm);

    /**
     * 根据考区代码查询考区名称
     *
     * @param kqdm 考区代码
     * @return 考区名称
     */
    String selectKqmcByCode(@Param("kqdm") String kqdm);
}