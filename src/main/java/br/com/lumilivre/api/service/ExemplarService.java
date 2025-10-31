package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;

@Service
public class ExemplarService {

    @Autowired
    private ExemplarRepository er;

    @Autowired
    private LivroRepository lr;

    @Autowired
    private ResponseModel rm;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    public ResponseEntity<?> buscarExemplaresPorIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        if (!lr.existsByIsbn(isbn)) {
            rm.setMensagem("Nenhum livro encontrado com esse ISBN.");
            return ResponseEntity.badRequest().body(rm);
        }
        List<ExemplarModel> exemplares = er.findAllByLivroIsbn(isbn);
        if (exemplares.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<ListaLivroDTO> exemplaresDTO = exemplares.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(exemplaresDTO);
    }

    @Transactional
    public ResponseEntity<?> cadastrar(ExemplarDTO dto) {
        rm.setMensagem("");

        if (dto.getLivro_id() == null) {
            rm.setMensagem("O ID do livro é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (er.existsByTombo(dto.getTombo())) {
            rm.setMensagem("Esse tombo já existe.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroOpt = lr.findById(dto.getLivro_id());
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Nenhum livro encontrado com o ID fornecido.");
            return ResponseEntity.badRequest().body(rm);
        }
        LivroModel livro = livroOpt.get();

        StatusLivro status;
        try {
            status = StatusLivro.valueOf(dto.getStatus_livro().toUpperCase());
        } catch (IllegalArgumentException e) {
            rm.setMensagem("Status do livro inválido.");
            return ResponseEntity.badRequest().body(rm);
        }

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(dto.getTombo());
        exemplar.setStatus_livro(status);
        exemplar.setLivro(livro);
        exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

        er.save(exemplar);

        atualizarQuantidadeExemplaresDoLivro(livro.getId());

        rm.setMensagem("Exemplar cadastrado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<?> atualizar(ExemplarDTO dto) {
        rm.setMensagem("");

        if (!er.existsById(dto.getTombo())) {
            rm.setMensagem("Exemplar com esse tombo não existe.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getLivro_id() == null) {
            rm.setMensagem("O ID do livro é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }
        Optional<LivroModel> livroOpt = lr.findById(dto.getLivro_id());
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Nenhum livro encontrado com o ID fornecido.");
            return ResponseEntity.badRequest().body(rm);
        }
        LivroModel livro = livroOpt.get();

        StatusLivro status;
        try {
            status = StatusLivro.valueOf(dto.getStatus_livro().toUpperCase());
        } catch (IllegalArgumentException e) {
            rm.setMensagem("Status do livro inválido.");
            return ResponseEntity.badRequest().body(rm);
        }

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(dto.getTombo());
        exemplar.setStatus_livro(status);
        exemplar.setLivro(livro);
        exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

        er.save(exemplar);

        atualizarQuantidadeExemplaresDoLivro(livro.getId());

        rm.setMensagem("Exemplar alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(String tombo) {
        Optional<ExemplarModel> exemplarOpt = er.findById(tombo);
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar com o tombo '" + tombo + "' não foi encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        ExemplarModel exemplar = exemplarOpt.get();
        LivroModel livro = exemplar.getLivro();

        if (livro != null && livro.getExemplares() != null) {
            livro.getExemplares().remove(exemplar);
        }

        er.delete(exemplar);

        if (livro != null) {
            Long novaQuantidade = er.countByLivroId(livro.getId());
            livro.setQuantidade(novaQuantidade.intValue());
            lr.save(livro);
        }

        rm.setMensagem("O exemplar foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ExemplarModel buscarPorTombo(String tombo) {
        return er.findByTombo(tombo).orElse(null);
    }

    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(Long livroId) {
        Long quantidade = er.countByLivroId(livroId);
        lr.findById(livroId).ifPresent(livro -> {
            livro.setQuantidade(quantidade.intValue());
            lr.save(livro);
        });
    }

    private ListaLivroDTO converterParaDTO(ExemplarModel exemplar) {
        LivroModel livro = exemplar.getLivro();

        String generosFormatados = livro.getGeneros().stream()
                .map(GeneroModel::getNome)
                .collect(Collectors.joining(", "));

        return new ListaLivroDTO(
                exemplar.getStatus_livro(),
                exemplar.getTombo(),
                livro.getIsbn(),
                livro.getCdd().getCodigo(),
                livro.getNome(),
                generosFormatados,
                livro.getAutor(),
                livro.getEditora(),
                exemplar.getLocalizacao_fisica());
    }
}