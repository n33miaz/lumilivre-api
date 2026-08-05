package br.com.lumilivre.api.enums;

/**
 * Tipos de evento de acesso registrados em {@link br.com.lumilivre.api.model.AccessLog}.
 *
 * <p>Os quatro primeiros são de <b>autenticação</b>: respondem "quem entrou, de
 * onde e quando". Os três últimos são de <b>uso</b>: respondem "o aluno usou a
 * biblioteca", que era o pedido do dono e não dava para responder só com login.
 *
 * <p>A lista é curta de propósito. O critério para admitir um evento novo está
 * em {@link br.com.lumilivre.api.security.AccessAudited}: se a pergunta que ele
 * responde é "quantos?", isso é métrica; a linha na tabela só se paga quando a
 * pergunta é "quem, quando, e em qual item".
 */
public enum AccessEvent {
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    TOKEN_REFRESH,
    ACCESS_DENIED,

    /** Consultou o acervo (catálogo, busca pública ou navegação por gênero). */
    CATALOG_SEARCH,

    /** Abriu a ficha de um livro específico. Alvo: id do livro. */
    BOOK_VIEWED,

    /** Abriu um comunicado/conteúdo do mural. Alvo: id do conteúdo. */
    CONTENT_VIEWED
}
