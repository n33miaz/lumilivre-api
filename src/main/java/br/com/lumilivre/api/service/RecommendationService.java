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
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_RECOMMENDATIONS = 10;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    /**
     * Recomendacoes da home do app.
     *
     * <p>{@code @Transactional(readOnly = true)}: o metodo navega
     * {@code loan -> bookCopy -> book -> genres} (colecao lazy) e, com
     * {@code spring.jpa.open-in-view=false}, a sessao ja estava fechada quando o
     * getter era chamado — a rota respondia 500 com LazyInitializationException
     * no stack local. Com a transacao aberta aqui, a colecao carrega dentro dela.
     *
     * <p>A chave do cache e a matricula, ou seja, o proprio recurso: dois leitores
     * nunca compartilham entrada. Quem garante que o chamador pode pedir esta
     * matricula e o {@code @CanAccessReader} no controller — sem ele o cache
     * serviria a lista de outro leitor com toda a fidelidade.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = MOBILE_RECOMMENDATIONS, key = "#matricula")
    public List<BookCardResponse> recommendForReader(String matricula) {
        List<Loan> historico = loanRepository.findByReader_RegistrationNumber(matricula);

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

        List<BookCardResponse> recomendacoes = bookRepository.findRecomendacoesPorGenero(
                List.copyOf(generos),
                jaLidos.isEmpty() ? List.of(UUID.randomUUID()) : jaLidos,
                PageRequest.of(0, MAX_RECOMMENDATIONS));

        if (recomendacoes.isEmpty()) {
            return bookRepository.findTopRated(PageRequest.of(0, MAX_RECOMMENDATIONS));
        }

        return Collections.unmodifiableList(recomendacoes);
    }
}
