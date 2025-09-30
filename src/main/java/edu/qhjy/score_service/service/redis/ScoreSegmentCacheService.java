package edu.qhjy.score_service.service.redis;

import edu.qhjy.score_service.domain.dto.ScoreSegmentDTO;
import edu.qhjy.score_service.domain.vo.GradeAdjustmentResultVO;
import edu.qhjy.score_service.domain.vo.ScoreSegmentOverviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 一分一段表缓存服务
 * 提供一分一段表数据的Redis缓存管理
 */
@Slf4j
@Service
public class ScoreSegmentCacheService {

    // 缓存键前缀
    private static final String OVERVIEW_CACHE_PREFIX = "score_segment:overview:";
    private static final String DETAIL_CACHE_PREFIX = "score_segment:detail:";
    private static final String CITY_CACHE_PREFIX = "score_segment:city:";
    private static final String CALCULATION_STATUS_PREFIX = "score_segment:status:";
    private static final String PRECOMPUTE_LOCK_PREFIX = "score_segment:lock:";
    private static final String GRADE_DISTRIBUTION_PREFIX = "score_segment:grade_distribution:";
    private static final String GRADE_THRESHOLDS_PREFIX = "score_segment:grade_thresholds:";
    private static final String SCORE_SEGMENT_DATA_PREFIX = "score_segment:data:";
    // 缓存过期时间
    private static final int OVERVIEW_CACHE_HOURS = 4;  // 总览数据缓存4小时
    private static final int DETAIL_CACHE_HOURS = 2;    // 详细数据缓存2小时
    private static final int STATUS_CACHE_MINUTES = 30; // 状态缓存30分钟
    private static final int LOCK_TIMEOUT_MINUTES = 10; // 预计算锁10分钟
    private static final int GRADE_DATA_CACHE_HOURS = 3; // 等级数据缓存3小时
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存总览数据
     */
    public void cacheOverviewData(String ksjhdm, String kmmc, ScoreSegmentOverviewVO data) {
        try {
            String key = buildOverviewKey(ksjhdm, kmmc, null);
            redisTemplate.opsForValue().set(key, data, Duration.ofHours(OVERVIEW_CACHE_HOURS));
            log.debug("缓存一分一段表总览数据: {}", key);
        } catch (Exception e) {
            log.error("缓存总览数据失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
    }

    /**
     * 获取缓存的总览数据
     */
    public ScoreSegmentOverviewVO getCachedOverviewData(String ksjhdm, String kmmc, String szsmc) {
        try {
            String key = buildOverviewKey(ksjhdm, kmmc, szsmc);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ScoreSegmentOverviewVO) {
                log.debug("命中一分一段表总览缓存: {}", key);
                return (ScoreSegmentOverviewVO) cached;
            }
        } catch (Exception e) {
            log.error("获取总览缓存失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
        return null;
    }

    /**
     * 缓存详细数据
     */
    public void cacheDetailData(String cacheKey, List<ScoreSegmentDTO> data) {
        try {
            String key = DETAIL_CACHE_PREFIX + cacheKey;
            redisTemplate.opsForValue().set(key, data, Duration.ofHours(DETAIL_CACHE_HOURS));
            log.debug("缓存一分一段表详细数据: {}", key);
        } catch (Exception e) {
            log.error("缓存详细数据失败: cacheKey={}", cacheKey, e);
        }
    }

    /**
     * 获取缓存的详细数据
     */
    @SuppressWarnings("unchecked")
    public List<ScoreSegmentDTO> getCachedDetailData(String cacheKey) {
        try {
            String key = DETAIL_CACHE_PREFIX + cacheKey;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("命中一分一段表详细缓存: {}", key);
                return (List<ScoreSegmentDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取详细缓存失败: cacheKey={}", cacheKey, e);
        }
        return null;
    }

    /**
     * 缓存分市州数据
     */
    public void cacheCityData(String ksjhdm, String kmmc, List<String> cities, List<ScoreSegmentOverviewVO> data) {
        try {
            String key = buildCityKey(ksjhdm, kmmc, cities);
            redisTemplate.opsForValue().set(key, data, Duration.ofHours(OVERVIEW_CACHE_HOURS));
            log.debug("缓存分市州一分一段表数据: {}", key);
        } catch (Exception e) {
            log.error("缓存分市州数据失败: ksjhdm={}, kmmc={}, cities={}", ksjhdm, kmmc, cities, e);
        }
    }

    /**
     * 获取缓存的分市州数据
     */
    @SuppressWarnings("unchecked")
    public List<ScoreSegmentOverviewVO> getCachedCityData(String ksjhdm, String kmmc, List<String> cities) {
        try {
            String key = buildCityKey(ksjhdm, kmmc, cities);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("命中分市州一分一段表缓存: {}", key);
                return (List<ScoreSegmentOverviewVO>) cached;
            }
        } catch (Exception e) {
            log.error("获取分市州缓存失败: ksjhdm={}, kmmc={}, cities={}", ksjhdm, kmmc, cities, e);
        }
        return null;
    }

    /**
     * 设置计算状态
     */
    public void setCalculationStatus(String ksjhdm, String kmmc, String status) {
        try {
            String key = buildStatusKey(ksjhdm, kmmc);
            redisTemplate.opsForValue().set(key, status, Duration.ofMinutes(STATUS_CACHE_MINUTES));
            log.debug("设置计算状态: {} = {}", key, status);
        } catch (Exception e) {
            log.error("设置计算状态失败: ksjhdm={}, kmmc={}, status={}", ksjhdm, kmmc, status, e);
        }
    }

    /**
     * 获取计算状态
     */
    public String getCalculationStatus(String ksjhdm, String kmmc) {
        try {
            String key = buildStatusKey(ksjhdm, kmmc);
            Object status = redisTemplate.opsForValue().get(key);
            return status != null ? status.toString() : "NOT_CALCULATED";
        } catch (Exception e) {
            log.error("获取计算状态失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
            return "ERROR";
        }
    }

    /**
     * 尝试获取预计算锁
     */
    public boolean tryLockPreCalculation(String ksjhdm, String kmmc) {
        try {
            String key = buildLockKey(ksjhdm, kmmc);
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", Duration.ofMinutes(LOCK_TIMEOUT_MINUTES));
            if (Boolean.TRUE.equals(success)) {
                log.debug("获取预计算锁成功: {}", key);
                return true;
            }
        } catch (Exception e) {
            log.error("获取预计算锁失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
        return false;
    }

    /**
     * 释放预计算锁
     */
    public void releaseLockPreCalculation(String ksjhdm, String kmmc) {
        try {
            String key = buildLockKey(ksjhdm, kmmc);
            redisTemplate.delete(key);
            log.debug("释放预计算锁: {}", key);
        } catch (Exception e) {
            log.error("释放预计算锁失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
    }

    /**
     * 缓存等级分布数据
     */
    public void cacheGradeDistribution(String ksjhdm, String kmmc, String szsmc, List<GradeAdjustmentResultVO.GradeDistributionData> data) {
        try {
            String key = buildGradeDistributionKey(ksjhdm, kmmc, szsmc);
            redisTemplate.opsForValue().set(key, data, Duration.ofHours(GRADE_DATA_CACHE_HOURS));
            log.debug("缓存等级分布数据: {}", key);
        } catch (Exception e) {
            log.error("缓存等级分布数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
    }

    /**
     * 获取缓存的等级分布数据
     */
    @SuppressWarnings("unchecked")
    public List<GradeAdjustmentResultVO.GradeDistributionData> getCachedGradeDistribution(String ksjhdm, String kmmc, String szsmc) {
        try {
            String key = buildGradeDistributionKey(ksjhdm, kmmc, szsmc);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("命中等级分布缓存: {}", key);
                return (List<GradeAdjustmentResultVO.GradeDistributionData>) cached;
            }
        } catch (Exception e) {
            log.error("获取等级分布缓存失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
        return null;
    }

    /**
     * 缓存等级分界线数据
     */
    public void cacheGradeThresholds(String ksjhdm, String kmmc, String szsmc, Map<String, BigDecimal> thresholds) {
        try {
            String key = buildGradeThresholdsKey(ksjhdm, kmmc, szsmc);
            redisTemplate.opsForValue().set(key, thresholds, Duration.ofHours(GRADE_DATA_CACHE_HOURS));
            log.debug("缓存等级分界线数据: {}", key);
        } catch (Exception e) {
            log.error("缓存等级分界线数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
    }

    /**
     * 缓存一分一段数据
     */
    public void cacheScoreSegmentData(String ksjhdm, String kmmc, String szsmc, List<ScoreSegmentDTO> data) {
        try {
            String key = buildScoreSegmentDataKey(ksjhdm, kmmc, szsmc);
            redisTemplate.opsForValue().set(key, data, Duration.ofHours(DETAIL_CACHE_HOURS));
            log.debug("缓存一分一段数据成功: key={}, 数据条数={}", key, data.size());
        } catch (Exception e) {
            log.error("缓存一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
    }

    /**
     * 获取缓存的等级分界线数据
     */
    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> getCachedGradeThresholds(String ksjhdm, String kmmc, String szsmc) {
        try {
            String key = buildGradeThresholdsKey(ksjhdm, kmmc, szsmc);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Map) {
                log.debug("命中等级分界线缓存: {}", key);
                return (Map<String, BigDecimal>) cached;
            }
        } catch (Exception e) {
            log.error("获取等级分界线缓存失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
        return null;
    }

    /**
     * 获取缓存的一分一段数据
     */
    @SuppressWarnings("unchecked")
    public List<ScoreSegmentDTO> getCachedScoreSegmentData(String ksjhdm, String kmmc, String szsmc) {
        try {
            String key = buildScoreSegmentDataKey(ksjhdm, kmmc, szsmc);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("从缓存获取一分一段数据成功: key={}", key);
                return (List<ScoreSegmentDTO>) cached;
            }
        } catch (Exception e) {
            log.error("从缓存获取一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
        return null;
    }

    /**
     * 清除一分一段表缓存
     */
    public void clearCache(String ksjhdm, String kmmc) {
        try {
            // 构建模式匹配键
            String overviewPattern = buildOverviewKey(ksjhdm, kmmc, "*");
            String detailPattern = DETAIL_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":*";
            String cityPattern = buildCityKey(ksjhdm, kmmc);
            String statusPattern = buildStatusKey(ksjhdm, kmmc);
            String gradeDistributionPattern = GRADE_DISTRIBUTION_PREFIX + ksjhdm + ":" + kmmc + ":*";
            String gradeThresholdsPattern = GRADE_THRESHOLDS_PREFIX + ksjhdm + ":" + kmmc + ":*";
            String scoreSegmentDataPattern = SCORE_SEGMENT_DATA_PREFIX + ksjhdm + ":" + kmmc + ":*";

            // 删除匹配的键
            deleteKeysByPattern(overviewPattern);
            deleteKeysByPattern(detailPattern);
            deleteKeysByPattern(gradeDistributionPattern);
            deleteKeysByPattern(gradeThresholdsPattern);
            deleteKeysByPattern(scoreSegmentDataPattern);
            redisTemplate.delete(cityPattern);
            redisTemplate.delete(statusPattern);

            log.info("清除一分一段表缓存: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
        } catch (Exception e) {
            log.error("清除缓存失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
    }

    /**
     * 清除指定市州的缓存
     */
    public void clearCityCache(String ksjhdm, String kmmc, String szsmc) {
        try {
            String overviewKey = buildOverviewKey(ksjhdm, kmmc, szsmc);
            String detailPattern = DETAIL_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc + ":*";
            String gradeDistributionKey = buildGradeDistributionKey(ksjhdm, kmmc, szsmc);
            String gradeThresholdsKey = buildGradeThresholdsKey(ksjhdm, kmmc, szsmc);
            String scoreSegmentDataKey = buildScoreSegmentDataKey(ksjhdm, kmmc, szsmc);

            redisTemplate.delete(overviewKey);
            redisTemplate.delete(gradeDistributionKey);
            redisTemplate.delete(gradeThresholdsKey);
            redisTemplate.delete(scoreSegmentDataKey);
            deleteKeysByPattern(detailPattern);

            log.info("清除市州缓存: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
        } catch (Exception e) {
            log.error("清除市州缓存失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
    }

    /**
     * 清除计算状态缓存
     */
    public void clearCalculationStatus(String ksjhdm, String kmmc) {
        try {
            String statusKey = buildStatusKey(ksjhdm, kmmc);
            redisTemplate.delete(statusKey);
            log.debug("清除计算状态缓存: {}", statusKey);
        } catch (Exception e) {
            log.error("清除计算状态缓存失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
    }

    /**
     * 清除预计算锁
     */
    public void clearPrecomputeLock(String ksjhdm, String kmmc) {
        try {
            String lockKey = buildLockKey(ksjhdm, kmmc);
            redisTemplate.delete(lockKey);
            log.debug("清除预计算锁: {}", lockKey);
        } catch (Exception e) {
            log.error("清除预计算锁失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
        }
    }

    /**
     * 根据模式删除键
     */
    private void deleteKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("删除缓存键: {} 个", keys.size());
            }
        } catch (Exception e) {
            log.error("删除缓存键失败: pattern={}", pattern, e);
        }
    }

    // 构建缓存键的辅助方法
    private String buildOverviewKey(String ksjhdm, String kmmc, String szsmc) {
        if (szsmc == null || szsmc.isEmpty()) {
            return OVERVIEW_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":all";
        }
        return OVERVIEW_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
    }

    private String buildCityKey(String ksjhdm, String kmmc, List<String> cities) {
        String cityStr = cities != null && !cities.isEmpty() ?
                cities.stream().sorted().collect(Collectors.joining(",")) : "all";
        return CITY_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":" + cityStr;
    }

    private String buildCityKey(String ksjhdm, String kmmc) {
        return CITY_CACHE_PREFIX + ksjhdm + ":" + kmmc + ":*";
    }

    private String buildStatusKey(String ksjhdm, String kmmc) {
        return CALCULATION_STATUS_PREFIX + ksjhdm + ":" + kmmc;
    }

    private String buildLockKey(String ksjhdm, String kmmc) {
        return PRECOMPUTE_LOCK_PREFIX + ksjhdm + ":" + kmmc;
    }

    private String buildGradeDistributionKey(String ksjhdm, String kmmc, String szsmc) {
        return GRADE_DISTRIBUTION_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
    }

    private String buildGradeThresholdsKey(String ksjhdm, String kmmc, String szsmc) {
        return GRADE_THRESHOLDS_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
    }

    private String buildScoreSegmentDataKey(String ksjhdm, String kmmc, String szsmc) {
        return SCORE_SEGMENT_DATA_PREFIX + ksjhdm + ":" + kmmc + ":" + szsmc;
    }
}