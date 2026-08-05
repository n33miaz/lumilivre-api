package br.com.lumilivre.api.dto.book;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ficha do livro. Rota pública desde a abertura do catálogo ao convidado, então
 * cada campo aqui é campo visível a anônimo.
 *
 * <p><b>Disponibilidade.</b> {@code totalCopies}/{@code availableCopies} são
 * contagens, e contagem não é dado de exemplar: nem tombo, nem prateleira, nem
 * status de um exemplar específico — nada que ajude a localizar fisicamente um
 * livro entra aqui. É o mesmo "disponível / emprestado" que qualquer OPAC de
 * biblioteca pública mostra na estante virtual, e sem ele o app não tinha como
 * dizer se dava para pedir emprestado: a ausência do campo era lida como zero e
 * o botão de solicitar empréstimo ficava morto em todo livro.
 *
 * <p><b>{@code updatedAt}.</b> Existe para o cliente invalidar o cache da capa.
 * Sem ele, trocar a capa de um livro não mudava a URL e o app seguia mostrando a
 * imagem antiga indefinidamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private UUID id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publicationDate;
    private Integer pageCount;
    private String synopsis;
    private String coverUrl;
    private String deweyCode;
    private LocalizedEnum ageRating;
    private LocalizedEnum coverType;
    private String edition;
    private Integer volume;
    private Double rating;
    private Set<String> genres;
    private OffsetDateTime updatedAt;
    private Long totalCopies;
    private Long availableCopies;
}
