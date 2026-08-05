package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import br.com.lumilivre.api.dto.dashboard.LoansByMonthResponse;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.service.BookService;
import br.com.lumilivre.api.service.DashboardService;
import br.com.lumilivre.api.service.LoanService;

class CacheConfigTest {

    private final GenericJackson2JsonRedisSerializer serializer = CacheConfig.redisValueSerializer();

    @Test
    void redisSerializerHandlesBookPublicationDate() {
        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("Dom Casmurro")
                .publisher("Editora")
                .publicationDate(LocalDate.of(1899, 1, 1))
                .build();

        Object result = serializer.deserialize(serializer.serialize(book));

        assertThat(result).isInstanceOf(Book.class);
        assertThat(((Book) result).getPublicationDate()).isEqualTo(LocalDate.of(1899, 1, 1));
    }

    @Test
    void redisSerializerHandlesDashboardLocalDateLists() {
        List<LoansByMonthResponse> value = new ArrayList<>();
        value.add(new LoansByMonthResponse(LocalDate.of(2026, 5, 1), 7L));

        Object result = serializer.deserialize(serializer.serialize(value));

        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).singleElement()
                .isInstanceOfSatisfying(LoansByMonthResponse.class, item -> {
                    assertThat(item.month()).isEqualTo(LocalDate.of(2026, 5, 1));
                    assertThat(item.total()).isEqualTo(7L);
                });
    }

    @Test
    void dashboardStatsAndLoanCounterDoNotShareCacheEntry() throws NoSuchMethodException {
        Cacheable dashboardStats = DashboardService.class
                .getDeclaredMethod("getStats")
                .getAnnotation(Cacheable.class);
        Cacheable activeOverdueCount = LoanService.class
                .getDeclaredMethod("getContagemEmprestimosAtivosEAtrasados")
                .getAnnotation(Cacheable.class);

        assertThat(dashboardStats.value()).containsExactly(CacheNames.DASHBOARD_STATS);
        assertThat(dashboardStats.key()).isEqualTo("'stats'");
        assertThat(activeOverdueCount.value()).containsExactly(CacheNames.DASHBOARD_ACTIVE_OVERDUE_COUNT);
    }

    @Test
    void bookDetailDoesNotCacheMissesAndItsUnlessExpressionActuallyEvaluates() throws NoSuchMethodException {
        // A ficha do livro virou pública. Como o cache é ConcurrentMapCache sem
        // limite de tamanho, guardar o resultado vazio de cada id inexistente
        // deixava um anônimo encher a heap com UUID aleatório — daí o `unless`.
        Cacheable bookDetail = BookService.class
                .getDeclaredMethod("findById", UUID.class)
                .getAnnotation(Cacheable.class);

        assertThat(bookDetail.value()).containsExactly(CacheNames.BOOK_DETAIL);
        assertThat(bookDetail.unless()).isNotBlank();

        // O Spring desembrulha o Optional antes de avaliar a expressão: #result é
        // o Book, ou null quando o Optional está vazio. Um `#result.isEmpty()`
        // aqui compila, passa em teste de anotação e estoura em produção,
        // transformando a ficha do livro num 500 — foi o que aconteceu.
        ExpressionParser parser = new SpelExpressionParser();
        Expression unless = parser.parseExpression(bookDetail.unless());

        StandardEvaluationContext found = new StandardEvaluationContext();
        found.setVariable("result", Book.builder().id(UUID.randomUUID()).title("Dom Casmurro").build());
        assertThat(unless.getValue(found, Boolean.class))
                .as("livro encontrado deve ser cacheado")
                .isFalse();

        StandardEvaluationContext missing = new StandardEvaluationContext();
        missing.setVariable("result", null);
        assertThat(unless.getValue(missing, Boolean.class))
                .as("id inexistente nao pode entrar no cache")
                .isTrue();
    }

    @Test
    void cacheErrorHandlerSwallowsBackendFailures() {
        CacheErrorHandler handler = new CacheConfig().errorHandler();
        Cache cache = new ConcurrentMapCache(CacheNames.DASHBOARD_STATS);
        RuntimeException backendDown = new RuntimeException("Unable to connect to Redis");

        // A cache backend outage must degrade to the source (DB), never bubble up
        // as a 500 from a @Cacheable endpoint such as /api/dashboard/*.
        assertThatCode(() -> handler.handleCacheGetError(backendDown, cache, "stats"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(backendDown, cache, "stats", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(backendDown, cache, "stats"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(backendDown, cache))
                .doesNotThrowAnyException();
    }
}
