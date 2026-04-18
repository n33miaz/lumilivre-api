package br.com.lumilivre.api.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.livro.LivroMobileResponse;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private static final int MAX_RECOMENDACOES = 10;

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;

    /**
     * Returns up to 10 book recommendations for the given student.
     * Strategy:
     *   1. Extract genres from the student's borrowing history.
     *   2. Recommend books in those genres not yet read (ordered by rating).
     *   3. If history is empty, fall back to top-rated books globally.
     */
    @Cacheable(value = "livro-detalhe", key = "'recomendacoes-' + #matricula")
    public List<LivroMobileResponse> recomendarParaAluno(String matricula) {
        List<EmprestimoModel> historico = emprestimoRepository.findByAluno_Matricula(matricula);

        if (historico.isEmpty()) {
            return livroRepository.findTopRated(PageRequest.of(0, MAX_RECOMENDACOES));
        }

        Set<String> generos = historico.stream()
                .flatMap(e -> e.getExemplar().getLivro().getGeneros().stream())
                .map(g -> g.getNome().toLowerCase())
                .collect(Collectors.toSet());

        List<Long> jaLidos = historico.stream()
                .map(e -> e.getExemplar().getLivro().getId())
                .distinct()
                .toList();

        if (generos.isEmpty()) {
            return livroRepository.findTopRated(PageRequest.of(0, MAX_RECOMENDACOES));
        }

        List<LivroMobileResponse> recomendacoes = livroRepository.findRecomendacoesPorGenero(
                List.copyOf(generos),
                jaLidos.isEmpty() ? List.of(-1L) : jaLidos,
                PageRequest.of(0, MAX_RECOMENDACOES));

        if (recomendacoes.isEmpty()) {
            return livroRepository.findTopRated(PageRequest.of(0, MAX_RECOMENDACOES));
        }

        return Collections.unmodifiableList(recomendacoes);
    }
}
