package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.googlebooks.GoogleBooksResponse;
import br.com.lumilivre.api.dto.googlebooks.ImageLinks;
import br.com.lumilivre.api.dto.googlebooks.VolumeInfo;
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

    // Injeção de dependência via construtor
    public GoogleBooksService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // DTO interno para o retorno combinado, tornando o serviço stateless
    public record GoogleBookData(LivroModel livro, List<String> categories) {
    }

    // O método agora se chama buscarDadosPorIsbn e retorna um Optional
    public Optional<GoogleBookData> buscarDadosPorIsbn(String isbn) {
        String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_BOOKS_API)
                .queryParam("q", "isbn:" + isbn)
                .toUriString();

        log.info("Buscando ISBN no Google Books: {}", isbn);

        try {
            // Usa os DTOs para deserialização automática e segura
            GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                log.warn("Nenhum livro encontrado para ISBN: {}", isbn);
                return Optional.empty();
            }

            VolumeInfo volumeInfo = response.items().get(0).volumeInfo();
            if (volumeInfo == null) {
                log.warn("VolumeInfo não encontrado para ISBN: {}", isbn);
                return Optional.empty();
            }

            LivroModel livro = new LivroModel();
            livro.setIsbn(isbn);
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

            List<String> categories = volumeInfo.categories() != null ? volumeInfo.categories()
                    : Collections.emptyList();

            log.info("Livro encontrado: {}", livro.getNome());
            return Optional.of(new GoogleBookData(livro, categories));

        } catch (Exception e) {
            log.error("Erro ao buscar livro no Google Books para ISBN {}: {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<LocalDate> parsearDataPublicacao(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(publishedDate));
        } catch (DateTimeParseException e1) {
            try {
                return Optional.of(YearMonth.parse(publishedDate).atDay(1));
            } catch (DateTimeParseException e2) {
                try {
                    return Optional.of(Year.parse(publishedDate).atDay(1));
                } catch (DateTimeParseException e3) {
                    log.warn("Formato de data não suportado: {}", publishedDate);
                    return Optional.empty();
                }
            }
        }
    }

    private Optional<String> obterUrlImagem(ImageLinks links) {
        if (links == null)
            return Optional.empty();
        return Optional.ofNullable(links.extraLarge())
                .or(() -> Optional.ofNullable(links.large()))
                .or(() -> Optional.ofNullable(links.medium()))
                .or(() -> Optional.ofNullable(links.thumbnail()))
                .or(() -> Optional.ofNullable(links.smallThumbnail()));
    }
}