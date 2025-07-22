package br.com.lumilivre.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import br.com.lumilivre.api.data.LivroDTO;

@Service
public class IsbnService {

    public LivroDTO buscarLivroPorIsbn(String isbn) {
        String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn;
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, LivroDTO.class);
    }
}