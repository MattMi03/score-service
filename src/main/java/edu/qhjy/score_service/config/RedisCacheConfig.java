package edu.qhjy.score_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis缓存配置类
 * 为不同类型的数据配置不同的缓存策略
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Autowired
    private Environment environment;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Redis连接工厂配置
     * 根据配置自动选择单体或集群模式
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 尝试多种方式获取集群节点配置
        List<String> clusterNodesList = null;

        // 方式1: 尝试获取数组格式
        try {
            String[] nodeArray = environment.getProperty("spring.data.redis.cluster.nodes", String[].class);
            if (nodeArray != null && nodeArray.length > 0) {
                clusterNodesList = Arrays.asList(nodeArray);
                log.info("通过数组格式读取到集群节点: {}", clusterNodesList);
            }
        } catch (Exception e) {
            log.debug("数组格式读取失败: {}", e.getMessage());
        }

        // 方式2: 如果数组格式失败，尝试逐个读取
        if (clusterNodesList == null || clusterNodesList.isEmpty()) {
            clusterNodesList = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) { // 最多尝试10个节点
                String node = environment.getProperty("spring.data.redis.cluster.nodes[" + i + "]");
                if (node != null && !node.trim().isEmpty()) {
                    clusterNodesList.add(node);
                } else {
                    break;
                }
            }
            if (!clusterNodesList.isEmpty()) {
                log.info("通过索引格式读取到集群节点: {}", clusterNodesList);
            }
        }

        // 方式3: 检查是否有任何cluster相关配置
        if (clusterNodesList == null || clusterNodesList.isEmpty()) {
            String nodesProperty = environment.getProperty("spring.data.redis.cluster.nodes");
            log.info("原始集群节点配置: {}", nodesProperty);
            if (nodesProperty != null && !nodesProperty.trim().isEmpty()) {
                // 尝试按逗号分割
                clusterNodesList = Arrays.asList(nodesProperty.split(","));
                log.info("通过逗号分割读取到集群节点: {}", clusterNodesList);
            }
        }

        log.info("Redis配置信息 - 集群节点: {}, 单体主机: {}:{}, 密码: {}",
                clusterNodesList, redisHost, redisPort,
                redisPassword != null && !redisPassword.trim().isEmpty() ? "已配置" : "未配置");

        // 判断是否配置了集群节点
        boolean hasValidClusterNodes = clusterNodesList != null &&
                !clusterNodesList.isEmpty() &&
                clusterNodesList.stream().anyMatch(node -> node != null && !node.trim().isEmpty());

        if (hasValidClusterNodes) {
            log.info("使用Redis集群模式，节点数量: {}", clusterNodesList.size());
            // 过滤掉空节点
            List<String> validNodes = clusterNodesList.stream()
                    .filter(node -> node != null && !node.trim().isEmpty())
                    .collect(Collectors.toList());

            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(validNodes);
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                clusterConfig.setPassword(redisPassword);
            }
            return new LettuceConnectionFactory(clusterConfig);
        } else {
            log.info("使用Redis单体模式，连接地址: {}:{}", redisHost, redisPort);
            // 单体模式
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
            standaloneConfig.setHostName(redisHost);
            standaloneConfig.setPort(redisPort);
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                standaloneConfig.setPassword(redisPassword);
            }
            standaloneConfig.setDatabase(redisDatabase);
            return new LettuceConnectionFactory(standaloneConfig);
        }
    }

    /**
     * RedisTemplate配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置Redis缓存管理器
     * 为不同的缓存区域设置不同的过期时间和配置
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)) // 默认15分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("score:");

        // 统计数据缓存配置 - 长期缓存（2小时）
        RedisCacheConfiguration statisticsConfig = defaultConfig
                .entryTtl(Duration.ofHours(2))
                .prefixCacheNameWith("score:statistics:");

        // 成绩列表缓存配置 - 中期缓存（30分钟）
        RedisCacheConfiguration scoreListConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("score:list:");

        // 个人成绩缓存配置 - 短期缓存（10分钟）
        RedisCacheConfiguration personalConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("score:personal:");

        // 趋势分析缓存配置 - 长期缓存（4小时）
        RedisCacheConfiguration trendsConfig = defaultConfig
                .entryTtl(Duration.ofHours(4))
                .prefixCacheNameWith("score:trends:");

        // 区域代码缓存配置 - 长期缓存（6小时）
        RedisCacheConfiguration areaCodeConfig = defaultConfig
                .entryTtl(Duration.ofHours(6))
                .prefixCacheNameWith("score:area_code:");

        // 子级统计缓存配置 - 中期缓存（1小时）
        RedisCacheConfiguration childrenCountConfig = defaultConfig
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("score:children_count:");

        // 科目缓存配置 - 长期缓存（6小时，科目数据相对稳定）
        RedisCacheConfiguration subjectConfig = defaultConfig
                .entryTtl(Duration.ofHours(6))
                .prefixCacheNameWith("score:subject:");

        // 配置不同缓存区域的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("scoreStatistics", statisticsConfig);
        cacheConfigurations.put("scoreDistribution", statisticsConfig);
        cacheConfigurations.put("scoreTrends", trendsConfig);
        cacheConfigurations.put("scoreList", scoreListConfig);
        cacheConfigurations.put("personalScore", personalConfig);
        cacheConfigurations.put("areaCode", areaCodeConfig);
        cacheConfigurations.put("childrenCount", childrenCountConfig);
        cacheConfigurations.put("subjectList", subjectConfig);
        cacheConfigurations.put("subjectEntities", subjectConfig);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}