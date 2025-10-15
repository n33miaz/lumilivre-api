package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.LivroModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoogleBooksService {

    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes";

    // novo campo para guardar as categorias
    private List<String> categories = new ArrayList<>();

    public LivroModel buscarLivroPorIsbn(String isbn) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_BOOKS_API)
                    .queryParam("q", "isbn:" + isbn)
                    .toUriString();

            System.out.println("Buscando ISBN no Google Books: " + url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // Verifica se a resposta é válida e contém items
            if (response == null || !response.containsKey("items")) {
                System.out.println("Nenhum livro encontrado para ISBN: " + isbn);
                return null;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items.isEmpty()) {
                System.out.println("Lista de items vazia para ISBN: " + isbn);
                return null;
            }

            Map<String, Object> primeiroItem = items.get(0);
            Map<String, Object> volumeInfo = (Map<String, Object>) primeiroItem.get("volumeInfo");

            if (volumeInfo == null) {
                System.out.println("VolumeInfo não encontrado para ISBN: " + isbn);
                return null;
            }

            if (volumeInfo.containsKey("categories")) {
                this.categories = (List<String>) volumeInfo.get("categories");
            } else {
                this.categories.clear();
            }

            LivroModel livro = new LivroModel();
            livro.setIsbn(isbn);
            livro.setNome((String) volumeInfo.get("title"));
            livro.setEditora((String) volumeInfo.get("publisher"));
            livro.setSinopse((String) volumeInfo.get("description"));

            // AUTORES: Lista completa separada por vírgula
            Object autoresObj = volumeInfo.get("authors");
            if (autoresObj instanceof List) {
                List<String> autores = ((List<?>) autoresObj).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());

                if (!autores.isEmpty()) {
                    String autoresString = String.join(", ", autores);
                    livro.setAutor(autoresString);
                    System.out.println("Autores encontrados: " + autoresString);
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
                                LocalDate.parse(publishedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao parsear data: " + publishedDate);
                }
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

            System.out.println("Livro encontrado: " + livro.getNome());
            return livro;

        } catch (Exception e) {
            System.out.println("Erro ao buscar livro no Google Books: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    public List<String> getCategories() {
        return this.categories;
    }
}

