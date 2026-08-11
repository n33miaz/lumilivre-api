package br.com.lumilivre.api.dto.loan;

/**
 * Contagem global de empréstimos por status, usada pelos cartões-aba da tela de
 * empréstimos. Existe porque a listagem é paginada no servidor: contar a partir
 * da página carregada mostraria o total da página (ex.: 7), não o total real
 * (ex.: 164). Cada campo casa com a semântica do filtro server-side equivalente.
 */
public record LoanStatusSummaryResponse(
        long all,
        long active,
        long overdue,
        long dueToday,
        long completed) {
}
