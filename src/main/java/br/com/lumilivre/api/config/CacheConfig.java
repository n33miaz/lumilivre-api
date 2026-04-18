package br.com.lumilivre.api.config;

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
            "dashboard_stats_emprestimos", Duration.ofMinutes(5),
            "dashboard_atrasados_count",   Duration.ofMinutes(5),
            "dashboard_atrasados_list",    Duration.ofMinutes(5),
            "dashboard_solicitacoes",      Duration.ofMinutes(3),
            "catalogo-mobile",             Duration.ofHours(1),
            "livro-detalhe",               Duration.ofMinutes(30)
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
                "dashboard_stats_emprestimos",
                "dashboard_atrasados_count",
                "dashboard_atrasados_list",
                "dashboard_solicitacoes",
                "contagem_livros",
                "contagem_alunos",
                "cdds",
                "generos-dto",
                "modulos",
                "turnos",
                "catalogo-mobile",
                "livro-detalhe");
    }
}
