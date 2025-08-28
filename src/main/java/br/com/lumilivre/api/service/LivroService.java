package br.com.lumilivre.api.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;

@Service
public class LivroService {

    @Autowired
    private ExemplarRepository er;

    @Autowired
    private LivroRepository lr;

    @Autowired
    private ResponseModel rm;

    @Autowired
    private AutorService as;

    @Autowired
    private GeneroService gs;

    // ------------------------ BUSCAS ------------------------
    public Page<ListaLivroDTO> buscarParaListaAdmin(Pageable pageable) {
        return lr.findLivrosParaListaAdmin(pageable);
    }

    public Page<ListaLivroDTO> buscarParaListaAdminComFiltro(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return lr.findLivrosParaListaAdmin(pageable);
        }
        return lr.findLivrosParaListaAdminComFiltro(texto, pageable);
    }

    public Iterable<LivroModel> buscarLivrosDisponiveis() {
        return lr.findLivrosDisponiveis();
    }

    public Page<LivroModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return lr.findAll(pageable);
        }
        return lr.buscarPorTexto(texto, pageable);
    }

    public Page<LivroModel> buscarAvancado(String nome, String isbn, String autor, String genero, String editora, Pageable pageable) {
        return lr.buscarAvancado(nome, isbn, autor, genero, editora, pageable);
    }

    public ResponseEntity<LivroModel> findByIsbn(String isbn) {
        Optional<LivroModel> livro = lr.findByIsbn(isbn);
        if (livro.isPresent()) {
            return ResponseEntity.ok(livro.get());
        }
        rm.setMensagem("Livro não encontrado.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    // ------------------------ CADASTRO ------------------------
    public ResponseEntity<?> cadastrar(LivroDTO dto) {
        rm.setMensagem("");

        if (dto.getIsbn() == null || dto.getIsbn().trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (lr.existsById(dto.getIsbn())) {
            rm.setMensagem("Esse ISBN já está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
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

    // ------------------------ ATUALIZAÇÃO ------------------------
    public ResponseEntity<?> atualizar(LivroDTO dto) {
        rm.setMensagem("");

        if (dto.getIsbn() == null || dto.getIsbn().trim().isEmpty()) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroExistente = lr.findByIsbn(dto.getIsbn());
        if (livroExistente.isEmpty()) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        LivroModel livro = livroExistente.get();
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
        livro.setAutor(as.buscarPorCodigo(dto.getAutor()));
        livro.setGenero(gs.buscarPorNome(dto.getGenero()));

        lr.save(livro);

        rm.setMensagem("Livro atualizado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // ------------------------ EXCLUSÕES ------------------------
    public ResponseEntity<ResponseModel> excluir(String isbn) {
        if (!lr.existsById(isbn)) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        lr.deleteById(isbn);
        rm.setMensagem("Livro removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<?> excluirLivroComExemplares(String isbn) {
        Optional<LivroModel> livroOpt = lr.findByIsbn(isbn);

        if (livroOpt.isEmpty()) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        er.deleteAllByLivroIsbn(isbn);
        lr.delete(livroOpt.get());

        rm.setMensagem("Livro e todos os exemplares foram removidos com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // ------------------------ QUANTIDADE DE EXEMPLARES ------------------------
    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(String isbn) {
        Long quantidade = er.contarExemplaresPorLivro(isbn);
        lr.findById(isbn).ifPresent(l -> {
            l.setQuantidade(quantidade.intValue());
            lr.save(l);
        });
    }
}
