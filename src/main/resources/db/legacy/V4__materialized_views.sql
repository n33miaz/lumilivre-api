-- V4: Materialized views for advanced management dashboard
-- Refreshed periodically by DashboardRefreshJob (every 15 minutes)

-- ============================================================
-- mv_dashboard_stats: Aggregated loan and request counts
-- ============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_dashboard_stats AS
SELECT
    COUNT(*) FILTER (WHERE e.status_emprestimo = 'ATIVO')    AS emprestimos_ativos,
    COUNT(*) FILTER (WHERE e.status_emprestimo = 'ATRASADO') AS emprestimos_atrasados,
    COUNT(*) FILTER (WHERE e.status_emprestimo = 'CONCLUIDO') AS emprestimos_concluidos,
    COALESCE(AVG(
        EXTRACT(EPOCH FROM (e.data_devolucao - e.data_emprestimo)) / 86400.0
    ) FILTER (WHERE e.status_emprestimo = 'CONCLUIDO'), 0) AS media_dias_devolucao,
    (SELECT COUNT(*) FROM solicitacao_emprestimo WHERE status = 'PENDENTE') AS solicitacoes_pendentes,
    (SELECT COUNT(*) FROM reserva WHERE status = 'AGUARDANDO')             AS reservas_aguardando
FROM emprestimo e
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS mv_dashboard_stats_idx ON mv_dashboard_stats ((1));

-- ============================================================
-- mv_top_livros: Most borrowed books (top 20)
-- ============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_top_livros AS
SELECT
    l.id            AS livro_id,
    l.nome          AS titulo,
    l.autor,
    COALESCE(l.imagem, '') AS imagem,
    COUNT(e.id)     AS total_emprestimos,
    COALESCE(l.avaliacao, 0) AS avaliacao
FROM livro l
JOIN exemplar ex ON ex.livro_id = l.id
JOIN emprestimo e ON e.exemplar_tombo = ex.tombo
GROUP BY l.id, l.nome, l.autor, l.imagem, l.avaliacao
ORDER BY total_emprestimos DESC
LIMIT 20
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS mv_top_livros_idx ON mv_top_livros (livro_id);

-- ============================================================
-- mv_emprestimos_por_mes: Monthly loan volume (last 12 months)
-- ============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_emprestimos_por_mes AS
SELECT
    DATE_TRUNC('month', e.data_emprestimo) AS mes,
    COUNT(*)                               AS total
FROM emprestimo e
WHERE e.data_emprestimo >= NOW() - INTERVAL '12 months'
GROUP BY mes
ORDER BY mes
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS mv_emprestimos_por_mes_idx ON mv_emprestimos_por_mes (mes);
