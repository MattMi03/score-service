package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.entity.XkbmkmEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考生报考科目Mapper (score数据库)
 */
@Mapper
public interface XkbmkmMapper {

    /**
     * 根据报名标识查询报考科目
     */
    List<XkbmkmEntity> selectByXkbmbs(@Param("xkbmbs") Long xkbmbs);

    /**
     * 根据科目名称查询报考科目
     */
    List<XkbmkmEntity> selectByKmmc(@Param("kmmc") String kmmc);

    /**
     * 根据报名标识和科目名称查询报考科目
     */
    XkbmkmEntity selectByXkbmbsAndKmmc(@Param("xkbmbs") Long xkbmbs, @Param("kmmc") String kmmc);

    /**
     * 根据报名标识列表查询报考科目
     */
    List<XkbmkmEntity> selectByXkbmbsList(@Param("xkbmbsList") List<Long> xkbmbsList);

    /**
     * 根据科目名称和报名标识列表查询报考科目
     */
    List<XkbmkmEntity> selectByKmmcAndXkbmbsList(@Param("kmmc") String kmmc,
                                                 @Param("xkbmbsList") List<Long> xkbmbsList);

    /**
     * 插入报考科目记录
     */
    int insert(XkbmkmEntity xkbmkmEntity);

    /**
     * 批量插入报考科目记录
     */
    int insertBatch(@Param("list") List<XkbmkmEntity> list);

    /**
     * 根据ID更新报考科目记录
     */
    int updateById(XkbmkmEntity xkbmkmEntity);

    /**
     * 根据ID删除报考科目记录
     */
    int deleteById(@Param("xkbmkmbs") Long xkbmkmbs);
}