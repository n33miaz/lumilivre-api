package br.com.lumilivre.api.dto.book;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Estado do interesse do leitor autenticado em um livro, devolvido pelo marcar
 * e pelo desmarcar.
 *
 * <p>Devolve estado e não mensagem para o cliente não precisar interpretar o
 * código HTTP: marcar duas vezes responde {@code interested=true} nas duas, com
 * o {@code markedAt} da primeira vez.
 *
 * <p>Não carrega quantas outras pessoas marcaram o mesmo livro. A contagem é
 * dado de comportamento de turma inteira e existe para decidir compra de
 * acervo, não para o aluno ver — quem a lê é a biblioteca, em
 * {@link BookInterestSummaryResponse}.
 */
public record BookInterestStateResponse(
        UUID bookId,
        boolean interested,
        OffsetDateTime markedAt) {

    public static BookInterestStateResponse marked(UUID bookId, OffsetDateTime markedAt) {
        return new BookInterestStateResponse(bookId, true, markedAt);
    }

    public static BookInterestStateResponse cleared(UUID bookId) {
        return new BookInterestStateResponse(bookId, false, null);
    }
}
