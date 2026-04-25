package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_LOANS_BY_MONTH;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_STATS;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_TOP_BOOKS;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.dashboard.DashboardStatsResponse;
import br.com.lumilivre.api.dto.dashboard.EmprestimosPorMesResponse;
import br.com.lumilivre.api.dto.dashboard.TopLivroResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    @Cacheable(DASHBOARD_STATS)
    public DashboardStatsResponse getStats() {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM mv_dashboard_stats");
        return new DashboardStatsResponse(
                toLong(row.get("active_loans")),
                toLong(row.get("overdue_loans")),
                toLong(row.get("completed_loans")),
                toDouble(row.get("avg_return_days")),
                toLong(row.get("pending_requests")),
                toLong(row.get("waiting_reservations")));
    }

    @Cacheable(DASHBOARD_TOP_BOOKS)
    public List<TopLivroResponse> getTopLivros() {
        return jdbc.query(
                "SELECT book_id, title, author, cover_url, total_loans, rating FROM mv_top_books",
                (rs, i) -> new TopLivroResponse(
                        rs.getObject("book_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("cover_url"),
                        rs.getLong("total_loans"),
                        rs.getDouble("rating")));
    }

    @Cacheable(DASHBOARD_LOANS_BY_MONTH)
    public List<EmprestimosPorMesResponse> getEmprestimosPorMes() {
        return jdbc.query(
                "SELECT month, total FROM mv_loans_by_month ORDER BY month",
                (rs, i) -> new EmprestimosPorMesResponse(
                        toLocalDate(rs.getObject("month")),
                        rs.getLong("total")));
    }

    /** Refreshes all materialized views. Runs every 15 minutes. */
    @Scheduled(fixedDelay = 900_000)
    public void refreshViews() {
        if (!isPostgres()) {
            log.debug("DashboardService: skipping materialized view refresh for non-PostgreSQL datasource.");
            return;
        }

        try {
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats");
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_books");
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_loans_by_month");
            log.info("DashboardService: materialized views refreshed.");
        } catch (Exception e) {
            log.error("DashboardService: failed to refresh materialized views: {}", e.getMessage());
        }
    }

    private boolean isPostgres() {
        try (var connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        } catch (Exception e) {
            log.warn("DashboardService: could not detect datasource type: {}", e.getMessage());
            return false;
        }
    }

    private long toLong(Object val) {
        return val instanceof Number n ? n.longValue() : 0L;
    }

    private double toDouble(Object val) {
        return val instanceof Number n ? n.doubleValue() : 0.0;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported month column type: " + value);
    }
}
