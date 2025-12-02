package br.com.lumilivre.api.service.infra;

import br.com.lumilivre.api.dto.integracao.google.GoogleBooksResponse;
import br.com.lumilivre.api.dto.integracao.google.ImageLinks;
import br.com.lumilivre.api.dto.integracao.google.VolumeInfo;
import br.com.lumilivre.api.model.LivroModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleBooksService {

    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes";
    private static final Logger log = LoggerFactory.getLogger(GoogleBooksService.class);

    private final RestTemplate restTemplate;

    public GoogleBooksService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record GoogleBookData(LivroModel livro, List<String> categories, Double averageRating) {
    }

    public Optional<GoogleBookData> buscarLivroInteligente(String isbn, String titulo, String autor) {
        if (isbn != null && !isbn.isBlank()) {
            Optional<GoogleBookData> porIsbn = buscarNaApi("isbn:" + isbn);
            if (porIsbn.isPresent()) {
                log.info("Livro encontrado via ISBN: {}", isbn);
                return porIsbn;
            }
        }

        if (titulo != null && !titulo.isBlank()) {
            String query = "intitle:" + titulo;
            if (autor != null && !autor.isBlank()) {
                query += "+inauthor:" + autor;
            }
            log.info("Tentando fallback por Título/Autor: {}", query);
            return buscarNaApi(query);
        }

        return Optional.empty();
    }

    private Optional<GoogleBookData> buscarNaApi(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_BOOKS_API)
                .queryParam("q", query)
                .queryParam("maxResults", 1)
                .queryParam("langRestrict", "pt")
                .toUriString();

        try {
            GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                return Optional.empty();
            }

            VolumeInfo volumeInfo = response.items().get(0).volumeInfo();
            return converterParaModel(volumeInfo);

        } catch (Exception e) {
            log.error("Erro na requisição Google Books: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GoogleBookData> converterParaModel(VolumeInfo volumeInfo) {
        if (volumeInfo == null)
            return Optional.empty();

        LivroModel livro = new LivroModel();
        livro.setNome(volumeInfo.title());
        livro.setEditora(volumeInfo.publisher());
        livro.setSinopse(volumeInfo.description());

        if (volumeInfo.authors() != null && !volumeInfo.authors().isEmpty()) {
            livro.setAutor(String.join(", ", volumeInfo.authors()));
        }

        if (volumeInfo.pageCount() != null) {
            livro.setNumero_paginas(volumeInfo.pageCount());
        }

        parsearDataPublicacao(volumeInfo.publishedDate()).ifPresent(livro::setData_lancamento);
        obterUrlImagem(volumeInfo.imageLinks()).ifPresent(livro::setImagem);

        Double rating = volumeInfo.averageRating();
        List<String> categories = volumeInfo.categories() != null ? volumeInfo.categories() : Collections.emptyList();

        return Optional.of(new GoogleBookData(livro, categories, rating));
    }

    private Optional<LocalDate> parsearDataPublicacao(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank())
            return Optional.empty();
        try {
            return Optional.of(LocalDate.parse(publishedDate));
        } catch (DateTimeParseException e1) {
            try {
                return Optional.of(YearMonth.parse(publishedDate).atDay(1));
            } catch (DateTimeParseException e2) {
                try {
                    return Optional.of(Year.parse(publishedDate).atDay(1));
                } catch (DateTimeParseException e3) {
                    return Optional.empty();
                }
            }
        }
    }

    private Optional<String> obterUrlImagem(ImageLinks links) {
        if (links == null)
            return Optional.empty();
        String url = Optional.ofNullable(links.extraLarge())
                .or(() -> Optional.ofNullable(links.large()))
                .or(() -> Optional.ofNullable(links.medium()))
                .or(() -> Optional.ofNullable(links.thumbnail()))
                .or(() -> Optional.ofNullable(links.smallThumbnail()))
                .orElse(null);

        if (url != null && url.startsWith("http://")) {
            url = url.replaceFirst("http://", "https://");
        }
        return Optional.ofNullable(url);
    }
}