package br.com.lumilivre.api.dto.book;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Card de livro das listas do app (catálogo, busca, gênero, recomendações e
 * interesses do leitor).
 *
 * <p>{@code updatedAt} entra por causa da capa: o cliente guarda a imagem em
 * cache pela URL, que não muda quando a bibliotecária troca a capa do livro. Sem
 * um carimbo de versão o app mostrava a capa antiga para sempre. É o mesmo campo
 * de {@link BookResponse}, aqui também porque as listas desenham capa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCardResponse {

    private UUID id;
    private String title;
    private String author;
    private String coverUrl;
    private Double rating;
    private OffsetDateTime updatedAt;
}
