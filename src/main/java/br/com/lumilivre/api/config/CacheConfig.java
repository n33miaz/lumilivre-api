package br.com.lumilivre.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
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
            "livro-detalhe"
        );
    }
}