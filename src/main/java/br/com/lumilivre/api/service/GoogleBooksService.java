package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.LivroModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class GoogleBooksService {

    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes";

    public LivroModel buscarLivroPorIsbn(String isbn) {
        RestTemplate restTemplate = new RestTemplate();
        String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_BOOKS_API)
                .queryParam("q", "isbn:" + isbn)
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("items")) return null;

        Map<String, Object> primeiroItem = ((List<Map<String, Object>>) response.get("items")).get(0);
        Map<String, Object> volumeInfo = (Map<String, Object>) primeiroItem.get("volumeInfo");

        LivroModel livro = new LivroModel();
        livro.setIsbn(isbn);
        livro.setNome((String) volumeInfo.get("title"));
        livro.setEditora((String) volumeInfo.get("publisher"));
        livro.setSinopse((String) volumeInfo.get("description"));

        // Autores
        Object autoresObj = volumeInfo.get("authors");
        if (autoresObj instanceof List) {
            List<?> autores = (List<?>) autoresObj;
            if (!autores.isEmpty()) {
                livro.setAutor(autores.get(0).toString());
            }
        }

        // Número de páginas
        Object pageCount = volumeInfo.get("pageCount");
        if (pageCount instanceof Number) {
            livro.setNumero_paginas(((Number) pageCount).intValue());
        }

        // Data de publicação
        String publishedDate = (String) volumeInfo.get("publishedDate");
        if (publishedDate != null) {
            try {
                if (publishedDate.length() == 4) {
                    livro.setData_lancamento(LocalDate.parse(publishedDate + "-01-01"));
                } else if (publishedDate.length() == 7) {
                    livro.setData_lancamento(LocalDate.parse(publishedDate + "-01"));
                } else {
                    livro.setData_lancamento(
                            LocalDate.parse(publishedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    );
                }
            } catch (Exception ignored) {}
        }

        // Imagens
        Object imageLinks = volumeInfo.get("imageLinks");
        if (imageLinks instanceof Map) {
            Map<String, String> img = (Map<String, String>) imageLinks;
            String imagem = img.getOrDefault("extraLarge",
                    img.getOrDefault("large",
                    img.getOrDefault("medium",
                    img.getOrDefault("thumbnail",
                    img.get("smallThumbnail")))));
            livro.setImagem(imagem);
        }

        // Campos opcionais
        livro.setNumero_capitulos(null);
        livro.setClassificacao_etaria(null);
        livro.setEdicao(null);
        livro.setVolume(null);

        return livro;
    }
}
