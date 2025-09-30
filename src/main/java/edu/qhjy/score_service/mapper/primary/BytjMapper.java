package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.BytjEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 毕业条件设置Mapper接口
 */
@Mapper
public interface BytjMapper {

    /**
     * 查询所有毕业条件设置
     *
     * @return 毕业条件设置列表
     */
    List<BytjEntity> selectAll();

    /**
     * 根据ID查询毕业条件设置
     *
     * @param bytjbs 毕业条件标识
     * @return 毕业条件设置
     */
    BytjEntity selectById(@Param("bytjbs") Long bytjbs);

    /**
     * 新增毕业条件设置
     *
     * @param entity 毕业条件设置实体
     * @return 影响行数
     */
    int insert(BytjEntity entity);

    /**
     * 更新毕业条件设置
     *
     * @param entity 毕业条件设置实体
     * @return 影响行数
     */
    int updateById(BytjEntity entity);

    /**
     * 删除毕业条件设置
     *
     * @param bytjbs 毕业条件标识
     * @return 影响行数
     */
    int deleteById(@Param("bytjbs") Long bytjbs);
}