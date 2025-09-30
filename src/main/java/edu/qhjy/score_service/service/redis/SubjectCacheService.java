package edu.qhjy.score_service.service.redis;

import edu.qhjy.score_service.domain.entity.KskmxxEntity;
import edu.qhjy.score_service.mapper.primary.KskmxxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 科目缓存服务
 * 提供科目数据的缓存管理功能
 *
 * @author system
 * @since 2025-09-05
 */
@Slf4j
@Service
public class SubjectCacheService {

    @Autowired
    private KskmxxMapper kskmxxMapper;

    /**
     * 获取指定考试计划和科目类型的科目名称列表（带缓存）
     *
     * @param ksjhdm 考试计划代码
     * @param kmlx   科目类型
     * @return 科目名称列表
     */
    @Cacheable(value = "subjectList", key = "#ksjhdm + '_' + #kmlx", unless = "#result.isEmpty()")
    public List<String> getSubjectNamesByKsjhdmAndKmlx(String ksjhdm, Integer kmlx) {
        log.info("从数据库查询科目列表，考试计划代码：{}，科目类型：{}", ksjhdm, kmlx);

        try {
            // 查询该考试计划下的所有科目
            List<KskmxxEntity> allSubjects = kskmxxMapper.selectByKsjhdm(ksjhdm);

            // 根据kmlx过滤科目，并按科目名称排序
            List<String> subjectNames = allSubjects.stream()
                    .filter(subject -> subject.getKmlx() != null && subject.getKmlx().equals(kmlx))
                    .map(KskmxxEntity::getKmmc)
                    .sorted() // 按科目名称排序
                    .collect(Collectors.toList());

            log.info("查询到{}个科目：{}", subjectNames.size(), subjectNames);
            return subjectNames;

        } catch (Exception e) {
            log.error("查询科目列表失败，考试计划代码：{}，科目类型：{}", ksjhdm, kmlx, e);
            throw new RuntimeException("查询科目列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定考试计划的所有科目实体列表（带缓存）
     *
     * @param ksjhdm 考试计划代码
     * @return 科目实体列表
     */
    @Cacheable(value = "subjectEntities", key = "#ksjhdm", unless = "#result.isEmpty()")
    public List<KskmxxEntity> getSubjectEntitiesByKsjhdm(String ksjhdm) {
        log.info("从数据库查询科目实体列表，考试计划代码：{}", ksjhdm);

        try {
            List<KskmxxEntity> subjects = kskmxxMapper.selectByKsjhdm(ksjhdm);
            log.info("查询到{}个科目实体", subjects.size());
            return subjects;

        } catch (Exception e) {
            log.error("查询科目实体列表失败，考试计划代码：{}", ksjhdm, e);
            throw new RuntimeException("查询科目实体列表失败：" + e.getMessage());
        }
    }

    /**
     * 清除指定考试计划的科目缓存
     *
     * @param ksjhdm 考试计划代码
     */
    @CacheEvict(value = {"subjectList", "subjectEntities"}, key = "#ksjhdm + '*'", allEntries = true)
    public void evictSubjectCache(String ksjhdm) {
        log.info("清除考试计划[{}]的科目缓存", ksjhdm);
    }

    /**
     * 清除所有科目缓存
     */
    @CacheEvict(value = {"subjectList", "subjectEntities"}, allEntries = true)
    public void evictAllSubjectCache() {
        log.info("清除所有科目缓存");
    }
}