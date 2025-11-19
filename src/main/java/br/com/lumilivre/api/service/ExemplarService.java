package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.ExemplarDTO;
import br.com.lumilivre.api.dto.ListaLivroDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

    public ResponseEntity<?> buscarExemplaresPorLivroId(Long livroId) {
        if (livroId == null) {
            return erro("O ID do livro é obrigatório.");
        }
        if (!livroRepository.existsById(livroId)) {
            return erro("Nenhum livro encontrado com o ID fornecido.");
        }

        List<ExemplarModel> exemplares = exemplarRepository.findAllByLivroIdWithDetails(livroId);

        if (exemplares.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<ListaLivroDTO> exemplaresDTO = exemplares.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(exemplaresDTO);
    }

    @Transactional
    public ResponseEntity<ResponseModel> cadastrar(ExemplarDTO dto) {
        if (dto.getLivro_id() == null) {
            return erro("O ID do livro é obrigatório.");
        }
        if (dto.getTombo() == null || dto.getTombo().isBlank()) {
            return erro("O tombo do exemplar é obrigatório.");
        }
        if (exemplarRepository.existsById(dto.getTombo())) {
            return erro("Já existe um exemplar com este tombo.");
        }

        Optional<LivroModel> livroOpt = livroRepository.findById(dto.getLivro_id());
        if (livroOpt.isEmpty()) {
            return erro("Nenhum livro encontrado com o ID fornecido.");
        }

        try {
            ExemplarModel exemplar = new ExemplarModel();
            exemplar.setTombo(dto.getTombo());
            exemplar.setStatus_livro(StatusLivro.valueOf(dto.getStatus_livro().toUpperCase()));
            exemplar.setLivro(livroOpt.get());
            exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

            exemplarRepository.save(exemplar);
            livroService.atualizarQuantidadeExemplaresDoLivro(livroOpt.get().getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ResponseModel("Exemplar cadastrado com sucesso."));
        } catch (IllegalArgumentException e) {
            return erro("Status do livro inválido. Valores permitidos: DISPONIVEL, EMPRESTADO, PERDIDO, DANIFICADO.");
        } catch (Exception e) {
            log.error("Erro ao cadastrar exemplar: {}", e.getMessage(), e);
            return erro("Erro interno ao cadastrar o exemplar.");
        }
    }

    @Transactional
    public ResponseEntity<ResponseModel> atualizar(String tombo, ExemplarDTO dto) {
        Optional<ExemplarModel> exemplarOpt = exemplarRepository.findById(tombo);
        if (exemplarOpt.isEmpty()) {
            return erro("Exemplar com o tombo '" + tombo + "' não foi encontrado.");
        }

        if (dto.getLivro_id() == null) {
            return erro("O ID do livro é obrigatório.");
        }
        Optional<LivroModel> livroOpt = livroRepository.findById(dto.getLivro_id());
        if (livroOpt.isEmpty()) {
            return erro("Nenhum livro encontrado com o ID fornecido.");
        }

        try {
            ExemplarModel exemplar = exemplarOpt.get();
            Long idLivroAntigo = exemplar.getLivro().getId();

            exemplar.setStatus_livro(StatusLivro.valueOf(dto.getStatus_livro().toUpperCase()));
            exemplar.setLivro(livroOpt.get());
            exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

            exemplarRepository.save(exemplar);

            livroService.atualizarQuantidadeExemplaresDoLivro(idLivroAntigo);
            if (!idLivroAntigo.equals(livroOpt.get().getId())) {
                livroService.atualizarQuantidadeExemplaresDoLivro(livroOpt.get().getId());
            }

            return ResponseEntity.ok(new ResponseModel("Exemplar alterado com sucesso."));
        } catch (IllegalArgumentException e) {
            return erro("Status do livro inválido.");
        } catch (Exception e) {
            log.error("Erro ao atualizar exemplar {}: {}", tombo, e.getMessage(), e);
            return erro("Erro interno ao atualizar o exemplar.");
        }
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(String tombo) {
        Optional<ExemplarModel> exemplarOpt = exemplarRepository.findById(tombo);
        if (exemplarOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseModel("Exemplar com o tombo '" + tombo + "' não foi encontrado."));
        }

        boolean estaEmprestado = emprestimoRepository.existsByExemplarTomboAndStatusEmprestimoIn(tombo,
                List.of(StatusEmprestimo.ATIVO, StatusEmprestimo.ATRASADO));

        if (estaEmprestado) {
            return erro(
                    "Não é possível excluir este exemplar, pois ele está associado a um empréstimo ativo ou atrasado.");
        }

        ExemplarModel exemplar = exemplarOpt.get();
        Long livroId = exemplar.getLivro().getId();

        exemplarRepository.delete(exemplar);
        livroService.atualizarQuantidadeExemplaresDoLivro(livroId);

        return ResponseEntity.ok(new ResponseModel("O exemplar foi removido com sucesso."));
    }

    public Optional<ExemplarModel> buscarPorTombo(String tombo) {
        return exemplarRepository.findById(tombo);
    }

    private ListaLivroDTO converterParaDTO(ExemplarModel exemplar) {
        LivroModel livro = exemplar.getLivro();
        if (livro == null) {
            return new ListaLivroDTO(exemplar.getStatus_livro(), exemplar.getTombo(), "N/A", "N/A",
                    "Livro não associado", "N/A", "N/A", "N/A", exemplar.getLocalizacao_fisica());
        }

        String generosFormatados = livro.getGeneros().stream()
                .map(GeneroModel::getNome)
                .collect(Collectors.joining(", "));

        return new ListaLivroDTO(
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

    private ResponseEntity<ResponseModel> erro(String mensagem) {
        return ResponseEntity.badRequest().body(new ResponseModel(mensagem));
    }
}