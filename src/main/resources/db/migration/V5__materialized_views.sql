-- ============================================================================
--  V5 - views materializadas do dashboard
-- ----------------------------------------------------------------------------
--  mv_dashboard_stats (contadores), mv_top_books (mais emprestados) e
--  mv_loans_by_month (serie mensal). O refresh periodico fica no
--  DashboardRefreshJob.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- mv_dashboard_stats: contadores agregados (1 linha)
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW mv_dashboard_stats AS
SELECT
    COUNT(*) FILTER (WHERE l.status = 'ACTIVE')    AS active_loans,
    COUNT(*) FILTER (WHERE l.status = 'OVERDUE')   AS overdue_loans,
    COUNT(*) FILTER (WHERE l.status = 'COMPLETED') AS completed_loans,
    COALESCE(AVG(
        EXTRACT(EPOCH FROM (l.returned_at - l.borrowed_at)) / 86400.0
    ) FILTER (WHERE l.status = 'COMPLETED' AND l.returned_at IS NOT NULL), 0) AS avg_return_days,
    (SELECT COUNT(*) FROM loan_request WHERE status = 'PENDING') AS pending_requests,
    (SELECT COUNT(*) FROM reservation  WHERE status = 'WAITING') AS waiting_reservations
FROM loan l
WITH DATA;

CREATE UNIQUE INDEX mv_dashboard_stats_idx ON mv_dashboard_stats ((1));

-- ----------------------------------------------------------------------------
-- mv_top_books: ranking de livros mais emprestados (top 20)
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW mv_top_books AS
SELECT
    b.id                                AS book_id,
    b.title                             AS title,
    b.author,
    COALESCE(b.cover_url, '')           AS cover_url,
    COUNT(l.id)                         AS total_loans,
    COALESCE(b.rating, 0)               AS rating
FROM book b
JOIN book_copy bc ON bc.book_id = b.id
JOIN loan l       ON l.book_copy_id = bc.id
WHERE b.deleted_at IS NULL
GROUP BY b.id, b.title, b.author, b.cover_url, b.rating
ORDER BY total_loans DESC
LIMIT 20
WITH DATA;

CREATE UNIQUE INDEX mv_top_books_idx ON mv_top_books (book_id);

-- ----------------------------------------------------------------------------
-- mv_loans_by_month: volume mensal nos ultimos 12 meses
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW mv_loans_by_month AS
SELECT
    DATE_TRUNC('month', l.borrowed_at) AS month,
    COUNT(*)                           AS total
FROM loan l
WHERE l.borrowed_at >= now() - INTERVAL '12 months'
GROUP BY month
ORDER BY month
WITH DATA;

CREATE UNIQUE INDEX mv_loans_by_month_idx ON mv_loans_by_month (month);
