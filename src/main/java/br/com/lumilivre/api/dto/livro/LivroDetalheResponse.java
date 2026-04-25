package br.com.lumilivre.api.dto.livro;

import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivroDetalheResponse {

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
    private String tipoCapa;
    private String classificacaoEtaria;
    private String cddCodigo;
    private String tipoCapaRaw;
    private String classificacaoEtariaRaw;
    private String edicao;
    private Integer volume;
    private Set<String> generos;
    private long exemplaresDisponiveis;
    private long totalExemplares;
    private Double avaliacao;

    public LivroDetalheResponse(Book livro, long exemplaresDisponiveis, long totalExemplares) {
        this.id = livro.getId();
        this.isbn = livro.getIsbn();
        this.nome = livro.getTitle();
        this.autor = livro.getAuthor();
        this.editora = livro.getPublisher();
        this.dataLancamento = livro.getPublicationDate();
        this.numeroPaginas = livro.getPageCount();
        this.sinopse = livro.getSynopsis();
        this.imagem = livro.getCoverUrl();
        this.edicao = livro.getEdition();
        this.volume = livro.getVolume();

        if (livro.getDeweyClassification() != null) {
            this.cdd = livro.getDeweyClassification().getDescription();
            this.cddCodigo = livro.getDeweyClassification().getCode();
        }

        if (livro.getCoverType() != null) {
            this.tipoCapa = livro.getCoverType().getStatus();
            this.tipoCapaRaw = livro.getCoverType().name();
        }

        if (livro.getAgeRating() != null) {
            this.classificacaoEtaria = livro.getAgeRating().getStatus();
            this.classificacaoEtariaRaw = livro.getAgeRating().name();
        }

        this.generos = livro.getGenres().stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());

        this.exemplaresDisponiveis = exemplaresDisponiveis;
        this.totalExemplares = totalExemplares;
        this.avaliacao = livro.getRating();
    }
}
