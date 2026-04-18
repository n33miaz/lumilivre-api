package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Map;

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

    @Cacheable("dashboard_stats_emprestimos")
    public DashboardStatsResponse getStats() {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM mv_dashboard_stats");
        return new DashboardStatsResponse(
                toLong(row.get("emprestimos_ativos")),
                toLong(row.get("emprestimos_atrasados")),
                toLong(row.get("emprestimos_concluidos")),
                toDouble(row.get("media_dias_devolucao")),
                toLong(row.get("solicitacoes_pendentes")),
                toLong(row.get("reservas_aguardando")));
    }

    @Cacheable("dashboard_stats_emprestimos")
    public List<TopLivroResponse> getTopLivros() {
        return jdbc.query(
                "SELECT livro_id, titulo, autor, imagem, total_emprestimos, avaliacao FROM mv_top_livros",
                (rs, i) -> new TopLivroResponse(
                        rs.getLong("livro_id"),
                        rs.getString("titulo"),
                        rs.getString("autor"),
                        rs.getString("imagem"),
                        rs.getLong("total_emprestimos"),
                        rs.getDouble("avaliacao")));
    }

    @Cacheable("dashboard_stats_emprestimos")
    public List<EmprestimosPorMesResponse> getEmprestimosPorMes() {
        return jdbc.query(
                "SELECT mes, total FROM mv_emprestimos_por_mes ORDER BY mes",
                (rs, i) -> new EmprestimosPorMesResponse(
                        rs.getDate("mes").toLocalDate(),
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
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_livros");
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_emprestimos_por_mes");
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
}
