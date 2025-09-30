package edu.qhjy.score_service.mapper.primary;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学校基本信息Mapper接口
 */
@Mapper
public interface XxjbxxMapper {

    /**
     * 根据学校名称查询学校代码
     *
     * @param xxmc 学校名称
     * @return 学校代码
     */
    String selectXxdmByName(@Param("xxmc") String xxmc);

    /**
     * 根据学校代码查询学校名称
     *
     * @param xxdm 学校代码
     * @return 学校名称
     */
    String selectXxmcByCode(@Param("xxdm") String xxdm);
}