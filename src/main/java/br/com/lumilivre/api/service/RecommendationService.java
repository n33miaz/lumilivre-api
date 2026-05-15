package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.MOBILE_RECOMMENDATIONS;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.v1.livro.LivroMobileResponse;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.BookRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_RECOMMENDATIONS = 10;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    @Cacheable(value = MOBILE_RECOMMENDATIONS, key = "#matricula")
    public List<LivroMobileResponse> recommendForStudent(String matricula) {
        List<Loan> historico = loanRepository.findByStudent_RegistrationNumber(matricula);

        if (historico.isEmpty()) {
            return bookRepository.findTopRated(PageRequest.of(0, MAX_RECOMMENDATIONS));
        }

        Set<String> generos = historico.stream()
                .flatMap(e -> e.getBookCopy().getBook().getGenres().stream())
                .map(g -> g.getName().toLowerCase())
                .collect(Collectors.toSet());

        List<UUID> jaLidos = historico.stream()
                .map(e -> e.getBookCopy().getBook().getId())
                .distinct()
                .toList();

        if (generos.isEmpty()) {
            return bookRepository.findTopRated(PageRequest.of(0, MAX_RECOMMENDATIONS));
        }

        List<LivroMobileResponse> recomendacoes = bookRepository.findRecomendacoesPorGenero(
                List.copyOf(generos),
                jaLidos.isEmpty() ? List.of(UUID.randomUUID()) : jaLidos,
                PageRequest.of(0, MAX_RECOMMENDATIONS));

        if (recomendacoes.isEmpty()) {
            return bookRepository.findTopRated(PageRequest.of(0, MAX_RECOMMENDATIONS));
        }

        return Collections.unmodifiableList(recomendacoes);
    }
}
