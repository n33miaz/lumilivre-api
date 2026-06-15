package br.com.lumilivre.api.config;

import static br.com.lumilivre.api.config.CacheNames.BOOK_COUNT;
import static br.com.lumilivre.api.config.CacheNames.BOOK_DETAIL;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_ACTIVE_OVERDUE_COUNT;
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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /** Per-cache TTLs; unlisted caches get DEFAULT_TTL. */
    private static final Map<String, Duration> CACHE_TTLS = Map.of(
            DASHBOARD_STATS, Duration.ofMinutes(5),
            DASHBOARD_ACTIVE_OVERDUE_COUNT, Duration.ofMinutes(5),
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
                                redisValueSerializer()));

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
                DASHBOARD_ACTIVE_OVERDUE_COUNT,
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

    static GenericJackson2JsonRedisSerializer redisValueSerializer() {
        return new GenericJackson2JsonRedisSerializer()
                .configure(mapper -> {
                    mapper.registerModule(new JavaTimeModule());
                    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                });
    }

    /**
     * Degrada o cache graciosamente: se o backend (Redis) estiver indisponível,
     * o erro é logado e ignorado em vez de propagar. Sem isto, qualquer falha de
     * conexao com o Redis transforma todo metodo @Cacheable num HTTP 500 — por
     * exemplo, os cards de "Analise gerencial" do dashboard ficavam vazios porque
     * /api/dashboard/* respondia 500 quando o Redis caia. Com o handler, a leitura
     * simplesmente busca na fonte (banco) e a escrita no cache vira no-op.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    static class LoggingCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache GET falhou [{}], buscando na fonte: {}", cache.getName(), exception.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache PUT falhou [{}] (valor nao cacheado): {}", cache.getName(), exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache EVICT falhou [{}]: {}", cache.getName(), exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache CLEAR falhou [{}]: {}", cache.getName(), exception.getMessage());
        }
    }
}
