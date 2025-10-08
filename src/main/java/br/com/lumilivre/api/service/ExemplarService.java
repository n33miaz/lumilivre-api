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

    /** Buscar todos os exemplares de um livro pelo ISBN */
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

        if (exemplares.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Converte a lista de Model para uma lista de DTO
        List<ListaLivroDTO> exemplaresDTO = exemplares.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(exemplaresDTO);
    }

    /** Cadastrar um exemplar */
    @Transactional
    public ResponseEntity<?> cadastrar(ExemplarDTO dto) {
        rm.setMensagem("");

        if (dto.getLivro_isbn() == null || dto.getLivro_isbn().trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
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

        LivroModel livro = lr.findByIsbn(dto.getLivro_isbn()).get();

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

    /** Atualizar um exemplar */
    @Transactional
    public ResponseEntity<?> atualizar(ExemplarDTO dto) {
        rm.setMensagem("");

        if (!er.existsById(dto.getTombo())) {
            rm.setMensagem("Exemplar com esse tombo não existe.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getLivro_isbn() == null || dto.getLivro_isbn().trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
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

        LivroModel livro = lr.findByIsbn(dto.getLivro_isbn()).get();

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

    /** Excluir um exemplar */
    @Transactional
    public ResponseEntity<ResponseModel> excluir(String tombo) {
        rm.setMensagem("");

        //  exemplar existe?
        Optional<ExemplarModel> exemplarOpt = er.findById(tombo);
        if (exemplarOpt.isEmpty()) {
            rm.setMensagem("Exemplar com o tombo '" + tombo + "' não foi encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        // ta em um empréstimo ativo?
        boolean temEmprestimoAtivo = emprestimoRepository.existsByExemplarTomboAndStatusEmprestimo(tombo, StatusEmprestimo.ATIVO);
        boolean temEmprestimoAtrasado = emprestimoRepository.existsByExemplarTomboAndStatusEmprestimo(tombo, StatusEmprestimo.ATRASADO);

        if (temEmprestimoAtivo || temEmprestimoAtrasado) {
            rm.setMensagem("Não é possível excluir. Este exemplar está atualmente emprestado.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(rm);
        }

        ExemplarModel exemplar = exemplarOpt.get();
        LivroModel livro = exemplar.getLivro_isbn(); 

        if (livro != null && livro.getExemplares() != null) {
            livro.getExemplares().remove(exemplar);
        }
        
        er.delete(exemplar); 
        
        if (livro != null) {
            Long novaQuantidade = er.contarExemplaresPorLivro(livro.getIsbn());
            livro.setQuantidade(novaQuantidade.intValue());
            lr.save(livro);
        }

        rm.setMensagem("O exemplar foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    /** Buscar um exemplar pelo tombo */
    public ExemplarModel buscarPorTombo(String tombo) {
        return er.findByTombo(tombo).orElse(null);
    }

    /** Atualizar a quantidade de exemplares de um livro */
    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(String isbn) {
        Long quantidade = er.contarExemplaresPorLivro(isbn);
        lr.findById(isbn).ifPresent(livro -> {
            livro.setQuantidade(quantidade.intValue());
            lr.save(livro);
        });
    }

    private ListaLivroDTO converterParaDTO(ExemplarModel exemplar) {
        LivroModel livro = exemplar.getLivro_isbn();
        return new ListaLivroDTO(
            exemplar.getStatus_livro(),
            exemplar.getTombo(),
            livro.getIsbn(),
            livro.getCdd(),
            livro.getNome(),
            livro.getGenero(),
            livro.getAutor(),
            livro.getEditora(),
            exemplar.getLocalizacao_fisica() 
        );
    }
}
