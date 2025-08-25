package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.data.EmprestimoResponseDTO;
import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
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

    public List<ExemplarModel> buscar() {
        return er.findAll();
    }


    public ResponseEntity<?> buscarExemplaresPorIsbn(String isbn) {
        rm.setMensagem("");

        if (isbn == null || isbn.trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (!lr.existsByIsbn(isbn)) {
            rm.setMensagem("Nenhum livro encontrado com esse ISBN.");
            return ResponseEntity.badRequest().body(rm);
        }

        List<ExemplarModel> exemplares = er.findAllByLivroIsbn(isbn);

        List<ExemplarDTO> resposta = exemplares.stream().map(exemplar -> {
            ExemplarDTO dto = new ExemplarDTO();
            dto.setTombo(exemplar.getTombo());
            dto.setStatus_livro(exemplar.getStatus_livro().toString());
            dto.setLivro_isbn(exemplar.getLivro_isbn().getIsbn());
            dto.setLocalizacao_fisica(exemplar.getLocalizacao_fisica());
            return dto;
        }).toList();

        return ResponseEntity.ok(resposta);
    }

    public ResponseEntity<?> excluirLivroComExemplares(String isbn) {
        rm.setMensagem("");

        Optional<LivroModel> livroOpt = lr.findByIsbn(isbn);
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        er.deleteAllByLivroIsbn(isbn);
        lr.deleteByIsbn(isbn);

        rm.setMensagem("Livro e todos os exemplares foram removidos com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<?> cadastrar(ExemplarDTO dto) {
        rm.setMensagem("");

        if (dto.getLivro_isbn() == null || dto.getLivro_isbn().trim().isEmpty()) {
            rm.setMensagem("A ISBN é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (er.existsByTombo(dto.getTombo())) {
            rm.setMensagem("Esse tombo já existe.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (!lr.existsByIsbn(dto.getLivro_isbn())) {
            rm.setMensagem("Esse ISBN não está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getStatus_livro() == null || dto.getStatus_livro().trim().isEmpty()) {
            rm.setMensagem("O Status do Livro é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        StatusLivro status;
        try {
            status = StatusLivro.valueOf(dto.getStatus_livro().toUpperCase());
        } catch (IllegalArgumentException e) {
            rm.setMensagem("Status do livro inválido.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroOpt = lr.findByIsbn(dto.getLivro_isbn());
        LivroModel livro = livroOpt.get();

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(dto.getTombo());
        exemplar.setStatus_livro(status);
        exemplar.setLivro_isbn(livro);
        exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

        er.save(exemplar);

        atualizarQuantidadeExemplaresDoLivro(livro.getIsbn());

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

        if (dto.getLivro_isbn() == null || dto.getLivro_isbn().trim().isEmpty()) {
            rm.setMensagem("A ISBN é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (!lr.existsByIsbn(dto.getLivro_isbn())) {
            rm.setMensagem("Esse ISBN não está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getStatus_livro() == null || dto.getStatus_livro().trim().isEmpty()) {
            rm.setMensagem("O Status do Livro é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        StatusLivro status;
        try {
            status = StatusLivro.valueOf(dto.getStatus_livro().toUpperCase());
        } catch (IllegalArgumentException e) {
            rm.setMensagem("Status do livro inválido.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroOpt = lr.findByIsbn(dto.getLivro_isbn());
        LivroModel livro = livroOpt.get();

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(dto.getTombo());
        exemplar.setStatus_livro(status);
        exemplar.setLivro_isbn(livro);
        exemplar.setLocalizacao_fisica(dto.getLocalizacao_fisica());

        er.save(exemplar);

        atualizarQuantidadeExemplaresDoLivro(livro.getIsbn());

        rm.setMensagem("Exemplar alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(String tombo) {
        rm.setMensagem("");

        Optional<ExemplarModel> exemplarOpt = er.findById(tombo);
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar com esse tombo não existe.");
            return ResponseEntity.badRequest().body(rm);
        }

        ExemplarModel exemplar = exemplarOpt.get();
        er.deleteById(tombo);

        atualizarQuantidadeExemplaresDoLivro(exemplar.getLivro_isbn().getIsbn());

        rm.setMensagem("O exemplar foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ExemplarModel buscarPorTombo(String tombo) {
        return er.findByTombo(tombo).orElse(null);
    }

    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(String isbn) {
        Long quantidade = er.contarExemplaresPorLivro(isbn);
        lr.findById(isbn).ifPresent(livro -> {
            livro.setQuantidade(quantidade.intValue());
            lr.save(livro);
        });
    }
}
