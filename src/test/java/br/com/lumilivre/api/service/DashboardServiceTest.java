package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @InjectMocks
    private DashboardService service;

    @Test
    void getStatsDeveMapearContadoresDaViewMaterializada() {
        when(jdbc.queryForMap("SELECT * FROM mv_dashboard_stats")).thenReturn(Map.of(
                "emprestimos_ativos", 4,
                "emprestimos_atrasados", 2L,
                "emprestimos_concluidos", 10,
                "media_dias_devolucao", 7.5,
                "solicitacoes_pendentes", 3,
                "reservas_aguardando", 1));

        var stats = service.getStats();

        assertThat(stats.emprestimosAtivos()).isEqualTo(4);
        assertThat(stats.emprestimosAtrasados()).isEqualTo(2);
        assertThat(stats.emprestimosConcluidos()).isEqualTo(10);
        assertThat(stats.mediaDiasDevolucao()).isEqualTo(7.5);
        assertThat(stats.solicitacoesPendentes()).isEqualTo(3);
        assertThat(stats.reservasAguardando()).isEqualTo(1);
    }

    @Test
    void refreshViewsDeveIgnorarDatasourceNaoPostgres() throws Exception {
        mockDatabaseProduct("H2");

        service.refreshViews();

        verify(jdbc, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void refreshViewsDeveAtualizarMaterializedViewsQuandoDatasourceForPostgres() throws Exception {
        mockDatabaseProduct("PostgreSQL");

        service.refreshViews();

        verify(jdbc).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats");
        verify(jdbc).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_livros");
        verify(jdbc).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_emprestimos_por_mes");
    }

    private void mockDatabaseProduct(String productName) throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
    }
}
