package edu.qhjy.score_service.service.redis;

import edu.qhjy.score_service.domain.dto.GradeThresholdsDTO;
import edu.qhjy.score_service.domain.dto.StudentScoreRankDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 等级计算缓存服务
 * 缓存中间计算结果，提升性能
 */
@Slf4j
@Service
public class GradeCalculationCacheService {

    private static final String THRESHOLD_CACHE_PREFIX = "grade_thresholds:";
    private static final String SCORE_RANK_CACHE_PREFIX = "score_ranks:";
    private static final String STATISTICS_CACHE_PREFIX = "grade_statistics:";
    private static final int CACHE_EXPIRATION_HOURS = 24;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存等级阈值
     *
     * @param ksjhdm     考试计划代码
     * @param kmmc       科目名称
     * @param szsmc      市州名称
     * @param thresholds 等级阈值
     */
    public void cacheThresholds(String ksjhdm, String kmmc, String szsmc,
                                GradeThresholdsDTO thresholds) {
        String cacheKey = THRESHOLD_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
        try {
            redisTemplate.opsForValue().set(cacheKey, thresholds,
                    Duration.ofHours(CACHE_EXPIRATION_HOURS));
            log.debug("等级阈值已缓存: {}", cacheKey);
        } catch (Exception e) {
            log.error("缓存等级阈值失败: {}", cacheKey, e);
        }
    }

    /**
     * 获取缓存的等级阈值
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 等级阈值，如果不存在则返回null
     */
    public GradeThresholdsDTO getCachedThresholds(String ksjhdm, String kmmc, String szsmc) {
        String cacheKey = THRESHOLD_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
        try {
            return (GradeThresholdsDTO) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("获取缓存的等级阈值失败: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 缓存学生成绩排名数据
     *
     * @param ksjhdm     考试计划代码
     * @param kmmc       科目名称
     * @param szsmc      市州名称
     * @param scoreRanks 成绩排名数据
     */
    public void cacheScoreRanks(String ksjhdm, String kmmc, String szsmc,
                                List<StudentScoreRankDTO> scoreRanks) {
        String cacheKey = SCORE_RANK_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
        try {
            redisTemplate.opsForValue().set(cacheKey, scoreRanks, Duration.ofMinutes(15));
            log.debug("缓存成绩排名数据: {}, 数量: {}", cacheKey, scoreRanks.size());
        } catch (Exception e) {
            log.error("缓存成绩排名数据失败: {}", cacheKey, e);
        }
    }

    /**
     * 获取缓存的成绩排名数据
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 成绩排名数据
     */
    @SuppressWarnings("unchecked")
    public List<StudentScoreRankDTO> getCachedScoreRanks(String ksjhdm, String kmmc, String szsmc) {
        String cacheKey = SCORE_RANK_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
        try {
            return (List<StudentScoreRankDTO>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("获取缓存成绩排名数据失败: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 缓存等级分布统计
     *
     * @param ksjhdm       考试计划代码
     * @param kmmc         科目名称
     * @param distribution 等级分布统计
     */
    public void cacheGradeDistribution(String ksjhdm, String kmmc,
                                       Map<String, Object> distribution) {
        String cacheKey = STATISTICS_CACHE_PREFIX + "distribution:" + ksjhdm + ":" + kmmc;
        try {
            redisTemplate.opsForValue().set(cacheKey, distribution, Duration.ofHours(6));
            log.debug("缓存等级分布统计: {}", cacheKey);
        } catch (Exception e) {
            log.error("缓存等级分布统计失败: {}", cacheKey, e);
        }
    }

    /**
     * 获取缓存的等级分布统计
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 等级分布统计
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedGradeDistribution(String ksjhdm, String kmmc) {
        String cacheKey = STATISTICS_CACHE_PREFIX + "distribution:" + ksjhdm + ":" + kmmc;
        try {
            return (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("获取缓存等级分布统计失败: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 清除相关缓存
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     */
    public void clearCache(String ksjhdm, String kmmc) {
        try {
            String pattern1 = THRESHOLD_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":*";
            String pattern2 = SCORE_RANK_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":*";
            String pattern3 = STATISTICS_CACHE_PREFIX + "*:" + ksjhdm + ":" + kmmc;

            redisTemplate.delete(redisTemplate.keys(pattern1));
            redisTemplate.delete(redisTemplate.keys(pattern2));
            redisTemplate.delete(redisTemplate.keys(pattern3));

            log.info("清除等级划分相关缓存: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
        } catch (Exception e) {
            log.error("清除缓存失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
    }
}