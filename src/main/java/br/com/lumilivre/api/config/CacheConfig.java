package br.com.lumilivre.api.config;

import static br.com.lumilivre.api.config.CacheNames.BOOK_COUNT;
import static br.com.lumilivre.api.config.CacheNames.BOOK_DETAIL;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_LOANS_BY_MONTH;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_LOAN_REQUESTS;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_COUNT;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_LIST;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_STATS;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_TOP_BOOKS;
import static br.com.lumilivre.api.config.CacheNames.MOBILE_CATALOG;
import static br.com.lumilivre.api.config.CacheNames.MOBILE_RECOMMENDATIONS;
import static br.com.lumilivre.api.config.CacheNames.STUDENT_COUNT;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /** Per-cache TTLs; unlisted caches get DEFAULT_TTL. */
    private static final Map<String, Duration> CACHE_TTLS = Map.of(
            DASHBOARD_STATS, Duration.ofMinutes(5),
            DASHBOARD_TOP_BOOKS, Duration.ofMinutes(5),
            DASHBOARD_LOANS_BY_MONTH, Duration.ofMinutes(5),
            DASHBOARD_OVERDUE_COUNT, Duration.ofMinutes(5),
            DASHBOARD_OVERDUE_LIST, Duration.ofMinutes(5),
            DASHBOARD_LOAN_REQUESTS, Duration.ofMinutes(3),
            MOBILE_CATALOG, Duration.ofHours(1),
            MOBILE_RECOMMENDATIONS, Duration.ofMinutes(30),
            BOOK_DETAIL, Duration.ofMinutes(30)
    );

    /**
     * Redis CacheManager — activated by setting spring.cache.type=redis
     * (or equivalently, LUMILIVRE_CACHE_TYPE=redis in env).
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();
        CACHE_TTLS.forEach((name, ttl) ->
                perCacheConfig.put(name, defaults.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }

    /**
     * Fallback CacheManager — in-memory, used in dev/test when Redis is not configured.
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager concurrentMapCacheManager() {
        return new ConcurrentMapCacheManager(
                DASHBOARD_STATS,
                DASHBOARD_TOP_BOOKS,
                DASHBOARD_LOANS_BY_MONTH,
                DASHBOARD_OVERDUE_COUNT,
                DASHBOARD_OVERDUE_LIST,
                DASHBOARD_LOAN_REQUESTS,
                BOOK_COUNT,
                STUDENT_COUNT,
                "cdds",
                "generos-dto",
                "modulos",
                "turnos",
                MOBILE_CATALOG,
                MOBILE_RECOMMENDATIONS,
                BOOK_DETAIL);
    }
}
