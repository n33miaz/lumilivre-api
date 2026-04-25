package br.com.lumilivre.api.dto.livro;

import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroResponse {

    private UUID id;
    private String isbn;
    private String nome;
    private String autor;
    private String editora;
    private LocalDate dataLancamento;
    private Integer numeroPaginas;
    private String sinopse;
    private String imagem;
    private String cdd;
    private String classificacaoEtaria;
    private String tipoCapa;
    private String edicao;
    private Integer volume;
    private Set<String> generos;

    public LivroResponse(Book livro) {
        this.id = livro.getId();
        this.isbn = livro.getIsbn();
        this.nome = livro.getTitle();
        this.autor = livro.getAuthor();
        this.editora = livro.getPublisher();
        this.dataLancamento = livro.getPublicationDate();
        this.numeroPaginas = livro.getPageCount();
        this.sinopse = livro.getSynopsis();
        this.imagem = livro.getCoverUrl();
        this.volume = livro.getVolume();
        this.edicao = livro.getEdition();

        this.cdd = (livro.getDeweyClassification() != null)
                ? livro.getDeweyClassification().getCode() + " - " + livro.getDeweyClassification().getDescription()
                : null;

        this.classificacaoEtaria = (livro.getAgeRating() != null)
                ? livro.getAgeRating().getStatus()
                : null;

        this.tipoCapa = (livro.getCoverType() != null)
                ? livro.getCoverType().getStatus()
                : null;

        this.generos = Optional.ofNullable(livro.getGenres())
                .orElse(Collections.emptySet())
                .stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());
    }
}
