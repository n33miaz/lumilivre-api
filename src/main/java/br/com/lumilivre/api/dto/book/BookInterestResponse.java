package br.com.lumilivre.api.dto.book;

import java.time.OffsetDateTime;

/**
 * Um item da lista de interesses do próprio leitor — o que substitui a lista de
 * favoritos guardada no {@code SharedPreferences} do celular.
 *
 * <p>Embrulha {@link BookCardResponse} em vez de repetir título/autor/capa:
 * é o mesmo card que o app já desenha no catálogo, e assim a capa continua
 * ganhando o {@code updatedAt} para invalidação de cache sem duplicar o campo.
 */
public record BookInterestResponse(
        BookCardResponse book,
        OffsetDateTime markedAt) {
}
