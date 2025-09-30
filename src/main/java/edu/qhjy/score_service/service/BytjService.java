package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.entity.BytjEntity;

import java.util.List;

/**
 * 毕业条件设置服务接口
 */
public interface BytjService {

    /**
     * 查询所有毕业条件设置
     *
     * @return 毕业条件设置列表
     */
    List<BytjEntity> selectAll();

    /**
     * 根据ID查询毕业条件设置
     *
     * @param bytjbs 毕业条件设置标识
     * @return 毕业条件设置实体
     */
    BytjEntity selectById(Long bytjbs);

    /**
     * 新增毕业条件设置
     *
     * @param bytjEntity 毕业条件设置实体
     * @return 新增结果
     */
    int insert(BytjEntity bytjEntity);

    /**
     * 根据ID更新毕业条件设置
     *
     * @param bytjEntity 毕业条件设置实体
     * @return 更新结果
     */
    int updateById(BytjEntity bytjEntity);

    /**
     * 根据ID删除毕业条件设置
     *
     * @param bytjbs 毕业条件设置标识
     * @return 删除结果
     */
    int deleteById(Long bytjbs);
}