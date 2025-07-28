package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Optional;

import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.AutorRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;

@Service
public class LivroService {

    @Autowired
    private ExemplarRepository er;
	
    @Autowired
    private LivroRepository lr;

    @Autowired
    private AutorRepository ar;

    @Autowired
    private ResponseModel rm;

    @Autowired
    private AutorService as;

    @Autowired
    private GeneroService gs;

    @Autowired
    private IsbnService isbnService;

    public Iterable<LivroModel> listar() {
        return lr.findAll();
    }

    public ResponseEntity<?> cadastrar(LivroDTO dto) {
        if (dto.getIsbn() == null || dto.getIsbn().trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (ar.existsById(dto.getIsbn())) {
            rm.setMensagem("Esse ISBN já está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            rm.setMensagem("O titulo é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getData_lancamento() == null) {
            rm.setMensagem("A data é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getData_lancamento().isAfter(java.time.LocalDate.now())) {
            rm.setMensagem("A data de lançamento não pode ser no futuro.");
            return ResponseEntity.badRequest().body(rm);
        }

        

        if (dto.getNumero_paginas() == null || dto.getNumero_paginas() <= 0) {
            rm.setMensagem("O número de páginas é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getEditora() == null || dto.getEditora().trim().isEmpty()) {
            rm.setMensagem("A editora é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getCdd() == null || dto.getCdd().trim().isEmpty()) {
            rm.setMensagem("O CDD é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getAutor() == null || dto.getAutor().trim().isEmpty()) {
            rm.setMensagem("O autor é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        AutorModel autor = as.buscarPorCodigo(dto.getAutor());
        if (autor == null) {
            rm.setMensagem("Autor não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getGenero() == null || dto.getGenero().trim().isEmpty()) {
            rm.setMensagem("O gênero é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        GeneroModel genero = gs.buscarPorNome(dto.getGenero());
        if (genero == null) {
            rm.setMensagem("Gênero não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        LivroModel livro = new LivroModel();
        livro.setIsbn(dto.getIsbn());
        livro.setNome(dto.getNome());
        livro.setData_lancamento(dto.getData_lancamento());
        livro.setNumero_paginas(dto.getNumero_paginas());
        livro.setCdd(Cdd.valueOf(dto.getCdd().toUpperCase()));
        livro.setEditora(dto.getEditora());
        livro.setNumero_capitulos(dto.getNumero_capitulos());
        livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
        livro.setEdicao(dto.getEdicao());
        livro.setVolume(dto.getVolume());
        livro.setQuantidade(dto.getQuantidade());
        livro.setSinopse(dto.getSinopse());
        livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        livro.setImagem(dto.getImagem());
        livro.setAutor(autor);
        livro.setGenero(genero);

        lr.save(livro);

        rm.setMensagem("Livro cadastrado com sucesso.");
        return ResponseEntity.status(HttpStatus.CREATED).body(rm);
    }

    public ResponseEntity<?> alterar(LivroDTO dto) {
        if (dto.getIsbn() == null || dto.getIsbn().trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroExistente = findById(dto.getIsbn());
        if (!livroExistente.isPresent()) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            rm.setMensagem("O título é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getData_lancamento() == null) {
            rm.setMensagem("A data é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }
        
        if (dto.getData_lancamento().isAfter(java.time.LocalDate.now())) {
            rm.setMensagem("A data de lançamento não pode ser no futuro.");
            return ResponseEntity.badRequest().body(rm);
        }


        if (dto.getNumero_paginas() == null || dto.getNumero_paginas() <= 0) {
            rm.setMensagem("O número de páginas é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getEditora() == null || dto.getEditora().trim().isEmpty()) {
            rm.setMensagem("A editora é obrigatória.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getCdd() == null || dto.getCdd().trim().isEmpty()) {
            rm.setMensagem("O CDD é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getAutor() == null || dto.getAutor().trim().isEmpty()) {
            rm.setMensagem("O autor é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        AutorModel autor = as.buscarPorCodigo(dto.getAutor());
        if (autor == null) {
            rm.setMensagem("Autor não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getGenero() == null || dto.getGenero().trim().isEmpty()) {
            rm.setMensagem("O gênero é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        GeneroModel genero = gs.buscarPorNome(dto.getGenero());
        if (genero == null) {
            rm.setMensagem("Gênero não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        LivroModel livro = livroExistente.get();
        livro.setIsbn(dto.getIsbn());
        livro.setNome(dto.getNome());
        livro.setData_lancamento(dto.getData_lancamento());
        livro.setNumero_paginas(dto.getNumero_paginas());
        livro.setCdd(Cdd.valueOf(dto.getCdd().toUpperCase()));
        livro.setEditora(dto.getEditora());
        livro.setNumero_capitulos(dto.getNumero_capitulos());
        livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
        livro.setEdicao(dto.getEdicao());
        livro.setVolume(dto.getVolume());
        livro.setQuantidade(dto.getQuantidade());
        livro.setSinopse(dto.getSinopse());
        livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        livro.setImagem(dto.getImagem());
        livro.setAutor(autor);
        livro.setGenero(genero);

        LivroModel salvo = lr.save(livro);

        rm.setMensagem("Livro atualizado com sucesso.");
        return ResponseEntity.status(HttpStatus.OK).body(rm);
    }

    private Optional<LivroModel> findById(String isbn) {
        return lr.findById(isbn);
    }

    public ResponseEntity<ResponseModel> deletar(String isbn) {
        lr.deleteById(isbn);
        rm.setMensagem("O Livro foi removido com sucesso.");
        return ResponseEntity.ok(rm);
    }
    
    public ResponseEntity<?> excluirLivroComExemplares(String isbn) {
        Optional<LivroModel> livroOpt = lr.findByIsbn(isbn);

        if (livroOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(rm);
        }

        LivroModel livro = livroOpt.get();

        er.deleteAllByLivroIsbn(isbn);

        lr.delete(livro);

        rm.setMensagem("Livro cadastrado com sucesso.");
        return ResponseEntity.ok(rm);

    }
    
}