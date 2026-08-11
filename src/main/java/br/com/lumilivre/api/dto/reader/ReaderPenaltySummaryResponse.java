package br.com.lumilivre.api.dto.reader;

/**
 * Contagem global de leitores por categoria de penalidade, usada pelos cartões
 * da tela de leitores. Existe pelo mesmo motivo do resumo de empréstimos: a
 * listagem é paginada no servidor, então contar a partir da página carregada
 * daria o total da página, não o global. "block" agrega BLOQUEIO + BANIMENTO, e
 * "noPenalty" cobre quem não tem penalidade que apareça nos cartões (inclui o
 * REGISTRO, tratado como "sem penalidade" pela tela).
 */
public record ReaderPenaltySummaryResponse(
        long noPenalty,
        long warning,
        long suspension,
        long block) {
}
