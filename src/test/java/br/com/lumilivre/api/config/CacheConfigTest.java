package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import br.com.lumilivre.api.dto.dashboard.LoansByMonthResponse;
import br.com.lumilivre.api.model.Book;
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
}
