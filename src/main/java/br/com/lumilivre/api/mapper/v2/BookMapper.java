package br.com.lumilivre.api.mapper.v2;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import br.com.lumilivre.api.dto.book.BookRequest;
import br.com.lumilivre.api.dto.book.BookResponse;
import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.dto.book.BookCatalogResponse;
import br.com.lumilivre.api.dto.book.BookGroupedResponse;
import br.com.lumilivre.api.dto.book.BookSummaryResponse;
import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.v1.genero.GeneroCatalogoResponse;
import br.com.lumilivre.api.dto.v1.livro.LivroDetalheResponse;
import br.com.lumilivre.api.dto.v1.livro.LivroAgrupadoResponse;
import br.com.lumilivre.api.dto.v1.livro.LivroListagemResponse;
import br.com.lumilivre.api.dto.v1.livro.LivroMobileResponse;
import br.com.lumilivre.api.dto.v1.livro.LivroRequest;
import br.com.lumilivre.api.dto.v1.livro.LivroResponse;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookMapper {

    private final EnumLabelResolver enumLabels;

    public BookResponse toResponse(Book b, Locale locale) {
        LocalizedEnum ageRating = b.getAgeRating() != null
                ? LocalizedEnum.of(b.getAgeRating(), enumLabels.resolve(b.getAgeRating(), locale))
                : null;
        LocalizedEnum coverType = b.getCoverType() != null
                ? LocalizedEnum.of(b.getCoverType(), enumLabels.resolve(b.getCoverType(), locale))
                : null;

        return BookResponse.builder()
                .id(b.getId())
                .isbn(b.getIsbn())
                .title(b.getTitle())
                .author(b.getAuthor())
                .publisher(b.getPublisher())
                .publicationDate(b.getPublicationDate())
                .pageCount(b.getPageCount())
                .synopsis(b.getSynopsis())
                .coverUrl(b.getCoverUrl())
                .deweyCode(b.getDeweyClassification() != null
                        ? b.getDeweyClassification().getCode() + " - " + b.getDeweyClassification().getDescription()
                        : null)
                .ageRating(ageRating)
                .coverType(coverType)
                .edition(b.getEdition())
                .volume(b.getVolume())
                .rating(b.getRating())
                .genres(Optional.ofNullable(b.getGenres())
                        .orElse(Collections.emptySet())
                        .stream().map(Genre::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    public BookResponse fromV1Detail(LivroDetalheResponse v1, Locale locale) {
        LocalizedEnum ageRating = resolveRawEnum(AgeRating.class, v1.getClassificacaoEtariaRaw(), locale);
        LocalizedEnum coverType = resolveRawEnum(CoverType.class, v1.getTipoCapaRaw(), locale);

        return BookResponse.builder()
                .id(v1.getId())
                .isbn(v1.getIsbn())
                .title(v1.getNome())
                .author(v1.getAutor())
                .publisher(v1.getEditora())
                .publicationDate(v1.getDataLancamento())
                .pageCount(v1.getNumeroPaginas())
                .synopsis(v1.getSinopse())
                .coverUrl(v1.getImagem())
                .deweyCode(v1.getCdd())
                .ageRating(ageRating)
                .coverType(coverType)
                .edition(v1.getEdicao())
                .volume(v1.getVolume())
                .rating(v1.getAvaliacao())
                .genres(v1.getGeneros())
                .build();
    }

    public BookResponse fromV1Response(LivroResponse v1, Locale locale) {
        return BookResponse.builder()
                .id(v1.getId())
                .isbn(v1.getIsbn())
                .title(v1.getNome())
                .author(v1.getAutor())
                .publisher(v1.getEditora())
                .publicationDate(v1.getDataLancamento())
                .pageCount(v1.getNumeroPaginas())
                .synopsis(v1.getSinopse())
                .coverUrl(v1.getImagem())
                .deweyCode(v1.getCdd())
                .edition(v1.getEdicao())
                .volume(v1.getVolume())
                .genres(v1.getGeneros())
                .build();
    }

    public BookSummaryResponse toSummary(LivroListagemResponse v1, Locale locale) {
        LocalizedEnum copyStatus = v1.getStatus() != null
                ? LocalizedEnum.of(v1.getStatus(), enumLabels.resolve(v1.getStatus(), locale))
                : null;

        return BookSummaryResponse.builder()
                .copyCode(v1.getTomboExemplar())
                .isbn(v1.getIsbn())
                .title(v1.getNome())
                .author(v1.getAutor())
                .publisher(v1.getEditora())
                .genre(v1.getGenero())
                .deweyCode(v1.getCdd())
                .physicalLocation(v1.getLocalizacao_fisica())
                .copyStatus(copyStatus)
                .build();
    }

    public BookGroupedResponse toGrouped(LivroAgrupadoResponse v1) {
        return BookGroupedResponse.builder()
                .id(v1.getId())
                .isbn(v1.getIsbn())
                .title(v1.getNome())
                .author(v1.getAutor())
                .publisher(v1.getEditora())
                .copyCount(v1.getQuantidade())
                .build();
    }

    public BookCardResponse toCard(LivroMobileResponse v1) {
        return BookCardResponse.builder()
                .id(v1.getId())
                .title(v1.getTitulo())
                .author(v1.getAutor())
                .coverUrl(v1.getImagem())
                .rating(v1.getAvaliacao())
                .build();
    }

    public BookCatalogResponse toCatalog(GeneroCatalogoResponse v1) {
        return BookCatalogResponse.builder()
                .genreName(v1.getNome())
                .books(v1.getLivros().stream().map(this::toCard).toList())
                .build();
    }

    public LivroRequest toV1Request(BookRequest req) {
        return LivroRequest.builder()
                .isbn(req.getIsbn())
                .nome(req.getTitle())
                .autor(req.getAuthor())
                .editora(req.getPublisher())
                .data_lancamento(req.getPublicationDate())
                .numero_paginas(req.getPageCount())
                .numero_capitulos(req.getChapterCount())
                .cdd(req.getDeweyCode())
                .classificacao_etaria(req.getAgeRating())
                .edicao(req.getEdition())
                .volume(req.getVolume())
                .quantidade(req.getCopyCount())
                .sinopse(req.getSynopsis())
                .tipo_capa(req.getCoverType())
                .imagem(req.getCoverUrl())
                .generos(req.getGenres())
                .avaliacao(req.getRating())
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> LocalizedEnum resolveRawEnum(Class<T> enumClass, String rawName, Locale locale) {
        if (rawName == null) return null;
        try {
            T value = Enum.valueOf(enumClass, rawName);
            return LocalizedEnum.of(value, enumLabels.resolve(value, locale));
        } catch (IllegalArgumentException e) {
            return new LocalizedEnum(rawName, rawName);
        }
    }
}
