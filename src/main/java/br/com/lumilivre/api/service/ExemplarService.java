package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.livro.ExemplarRequest;
import br.com.lumilivre.api.dto.livro.LivroListagemResponse;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExemplarService {

    private static final Logger log = LoggerFactory.getLogger(ExemplarService.class);

    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final LivroService livroService;

    public ExemplarService(ExemplarRepository er, LivroRepository lr, EmprestimoRepository emprestimoRepository,
            LivroService livroService) {
        this.exemplarRepository = er;
        this.livroRepository = lr;
        this.emprestimoRepository = emprestimoRepository;
        this.livroService = livroService;
    }

    public List<ExemplarModel> buscarTodos() {
        return exemplarRepository.findAll();
    }

    public List<LivroListagemResponse> buscarExemplaresPorLivroId(Long livroId) {
        if (livroId == null) {
            throw new RegraDeNegocioException("O ID do livro é obrigatório.");
        }
        if (!livroRepository.existsById(livroId)) {
            throw new RecursoNaoEncontradoException("Nenhum livro encontrado com o ID fornecido.");
        }

        List<ExemplarModel> exemplares = exemplarRepository.findAllByLivroIdWithDetails(livroId);

        return exemplares.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cadastrar(ExemplarRequest dto) {
        validarDadosExemplar(dto);

        if (exemplarRepository.existsById(dto.getTombo())) {
            throw new RegraDeNegocioException("Já existe um exemplar com este tombo.");
        }

        LivroModel livro = livroRepository.findById(dto.getLivro_id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum livro encontrado com o ID fornecido."));

        try {
            ExemplarModel exemplar = new ExemplarModel();
            exemplar.setTombo(dto.getTombo());
            exemplar.setStatus_livro(parseStatusLivro(dto.getStatus_livro()));
            exemplar.setLivro(livro);
            exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

            exemplarRepository.save(exemplar);
            livroService.atualizarQuantidadeExemplaresDoLivro(livro.getId());

        } catch (Exception e) {
            log.error("Erro ao cadastrar exemplar: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao cadastrar o exemplar: " + e.getMessage());
        }
    }

    @Transactional
    public void atualizar(String tombo, ExemplarRequest dto) {
        ExemplarModel exemplar = exemplarRepository.findById(tombo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exemplar com o tombo '" + tombo + "' não foi encontrado."));

        if (dto.getLivro_id() == null) {
            throw new RegraDeNegocioException("O ID do livro é obrigatório.");
        }

        LivroModel livroNovo = livroRepository.findById(dto.getLivro_id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum livro encontrado com o ID fornecido."));

        try {
            Long idLivroAntigo = exemplar.getLivro().getId();

            exemplar.setStatus_livro(parseStatusLivro(dto.getStatus_livro()));
            exemplar.setLivro(livroNovo);
            exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

            exemplarRepository.save(exemplar);

            livroService.atualizarQuantidadeExemplaresDoLivro(idLivroAntigo);
            if (!idLivroAntigo.equals(livroNovo.getId())) {
                livroService.atualizarQuantidadeExemplaresDoLivro(livroNovo.getId());
            }

        } catch (Exception e) {
            log.error("Erro ao atualizar exemplar {}: {}", tombo, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao atualizar o exemplar.");
        }
    }

    @Transactional
    public void excluir(String tombo) {
        ExemplarModel exemplar = exemplarRepository.findById(tombo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exemplar com o tombo '" + tombo + "' não foi encontrado."));

        boolean estaEmprestado = emprestimoRepository.existsByExemplarTomboAndStatusEmprestimoIn(tombo,
                List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));

        if (estaEmprestado) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir este exemplar, pois ele está associado a um empréstimo ativo ou atrasado.");
        }

        Long livroId = exemplar.getLivro().getId();
        exemplarRepository.delete(exemplar);
        livroService.atualizarQuantidadeExemplaresDoLivro(livroId);
    }

    // --- METODOS AUXILIARES ---

    private void validarDadosExemplar(ExemplarRequest dto) {
        if (dto.getLivro_id() == null) {
            throw new RegraDeNegocioException("O ID do livro é obrigatório.");
        }
        if (dto.getTombo() == null || dto.getTombo().isBlank()) {
            throw new RegraDeNegocioException("O tombo do exemplar é obrigatório.");
        }
    }

    private StatusLivro parseStatusLivro(String status) {
        try {
            return StatusLivro.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraDeNegocioException(
                    "Status do livro inválido. Valores permitidos: DISPONIVEL, EMPRESTADO, INDISPONIVEL, EM_MANUTENCAO.");
        }
    }

    private LivroListagemResponse converterParaDTO(ExemplarModel exemplar) {
        LivroModel livro = exemplar.getLivro();
        if (livro == null) {
            return new LivroListagemResponse(exemplar.getStatus_livro(), exemplar.getTombo(), "N/A", "N/A",
                    "Livro não associado", "N/A", "N/A", "N/A", exemplar.getLocalizacao_fisica());
        }

        String generosFormatados = livro.getGeneros().stream()
                .map(GeneroModel::getNome)
                .collect(Collectors.joining(", "));

        return new LivroListagemResponse(
                exemplar.getStatus_livro(),
                exemplar.getTombo(),
                livro.getIsbn(),
                livro.getCdd() != null ? livro.getCdd().getCodigo() : "N/A",
                livro.getNome(),
                generosFormatados,
                livro.getAutor(),
                livro.getEditora(),
                exemplar.getLocalizacao_fisica());
    }
}