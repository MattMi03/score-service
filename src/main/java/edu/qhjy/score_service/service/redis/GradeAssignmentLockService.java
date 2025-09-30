package edu.qhjy.score_service.service.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 等级划分分布式锁服务
 * 防止同一考试计划的重复处理
 */
@Slf4j
@Service
public class GradeAssignmentLockService {

    private static final String LOCK_PREFIX = "grade_assignment_lock:";
    private static final int LOCK_TIMEOUT = 300; // 5分钟超时
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取分布式锁
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 是否成功获取锁
     */
    public boolean acquireLock(String ksjhdm, String kmmc) {
        String lockKey = LOCK_PREFIX + ksjhdm + ":" + kmmc;
        String lockValue = UUID.randomUUID().toString();

        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_TIMEOUT));

            if (Boolean.TRUE.equals(success)) {
                log.info("成功获取等级划分锁: {}", lockKey);
                return true;
            } else {
                log.warn("获取等级划分锁失败，可能正在处理中: {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("获取分布式锁异常: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     */
    public void releaseLock(String ksjhdm, String kmmc) {
        String lockKey = LOCK_PREFIX + ksjhdm + ":" + kmmc;
        try {
            redisTemplate.delete(lockKey);
            log.info("释放等级划分锁: {}", lockKey);
        } catch (Exception e) {
            log.error("释放分布式锁异常: {}", lockKey, e);
        }
    }

    /**
     * 检查锁是否存在
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @return 锁是否存在
     */
    public boolean isLocked(String ksjhdm, String kmmc) {
        String lockKey = LOCK_PREFIX + ksjhdm + ":" + kmmc;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("检查锁状态异常: {}", lockKey, e);
            return false;
        }
    }
}