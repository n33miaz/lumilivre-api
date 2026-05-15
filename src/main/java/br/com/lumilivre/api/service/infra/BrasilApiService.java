package br.com.lumilivre.api.service.infra;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.integration.brasilapi.BrasilApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class BrasilApiService {

    private static final String BRASIL_API_URL = "https://brasilapi.com.br/api/isbn/v1/";
    private static final Logger log = LoggerFactory.getLogger(BrasilApiService.class);

    private final RestTemplate restTemplate;

    public BrasilApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "brasilApi", fallbackMethod = "buscarPorIsbnFallback")
    @Retry(name = "brasilApi")
    public Optional<BrasilApiResponse> buscarPorIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }

        String isbnLimpo = isbn.replaceAll("[^0-9]", "");
        String url = BRASIL_API_URL + isbnLimpo;

        try {
            BrasilApiResponse response = restTemplate.getForObject(url, BrasilApiResponse.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Livro não encontrado na BrasilAPI para o ISBN: {}", isbn);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<BrasilApiResponse> buscarPorIsbnFallback(String isbn, Exception e) {
        log.warn("BrasilAPI indisponível para ISBN '{}': {}", isbn, e.getMessage());
        return Optional.empty();
    }
}
