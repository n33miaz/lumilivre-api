package br.com.lumilivre.api.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.enums.StatusLivro;
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

    public Iterable<ExemplarModel> listar() {
        return er.findAll();
    }

    public ResponseEntity<?> cadastrar(ExemplarDTO dto) {
        rm.setMensagem("");

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
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Esse ISBN não está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        LivroModel livro = livroOpt.get();

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(dto.getTombo());
        exemplar.setStatus_livro(status);
        exemplar.setLivro_isbn(livro);

        er.save(exemplar);

        rm.setMensagem("Exemplar cadastrado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<?> alterar(ExemplarDTO dto) {
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
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Esse ISBN não está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        LivroModel livro = livroOpt.get();

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(dto.getTombo());
        exemplar.setStatus_livro(status);
        exemplar.setLivro_isbn(livro);

        er.save(exemplar);

        rm.setMensagem("Exemplar alterado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ResponseEntity<ResponseModel> deletar(String tombo) {
        rm.setMensagem("");

        if (!er.existsById(tombo)) {
            rm.setMensagem("Exemplar com esse tombo não existe.");
            return ResponseEntity.badRequest().body(rm);
        }

        er.deleteById(tombo);
        rm.setMensagem("O exemplar foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    public ExemplarModel buscarPorTombo(String tombo) {
        return er.findByTombo(tombo).orElse(null);
    }
}
