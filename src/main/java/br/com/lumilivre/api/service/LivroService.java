package br.com.lumilivre.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.data.LivroResponseMobileGeneroDTO;
import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.utils.UrlUtils;

@Service
public class LivroService {

    @Autowired private ExemplarRepository er;
    @Autowired private LivroRepository lr;
    @Autowired private ResponseModel rm;
    @Autowired private SupabaseStorageService storageService;
    @Autowired private GoogleBooksService googleBooksService;

    private final String BASE_URL_CAPAS = "https://ylwmaozotaddmyhosiqc.supabase.co/storage/v1/object/capas/livros";

    // ------------------------ BUSCAS ------------------------
    public Page<ListaLivroDTO> buscarParaListaAdmin(Pageable pageable) {
        return lr.findLivrosParaListaAdmin(pageable);
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

    public Page<LivroModel> buscarAvancado(String nome, String isbn, String autor, String genero, String editora,
            Pageable pageable) {
        return lr.buscarAvancado(nome, isbn, autor, genero, editora, pageable);
    }

    public ResponseEntity<LivroModel> findByIsbn(String isbn) {
        return lr.findByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    public ResponseEntity<List<LivroResponseMobileGeneroDTO>> listarPorGenero(String genero) {
        List<LivroModel> livros = lr.findByGeneroNomeIgnoreCase(genero);

        List<LivroResponseMobileGeneroDTO> resposta = livros.stream()
                .map(l -> new LivroResponseMobileGeneroDTO(
                        l.getImagem(),
                        l.getNome(),
                        l.getAutor()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resposta);
    }

    // ------------------------ CADASTRO ------------------------
    public ResponseEntity<?> cadastrar(LivroDTO dto, MultipartFile file) {
        rm.setMensagem("");

        if (isVazio(dto.getIsbn())) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (lr.existsById(dto.getIsbn())) {
            rm.setMensagem("Esse ISBN já está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        preencherComGoogleBooks(dto);

        ResponseEntity<?> erroValidacao = validarCampos(dto);
        if (erroValidacao != null) return erroValidacao;

        LivroModel livro = montarLivro(dto, file);

        lr.save(livro);
        rm.setMensagem("Livro cadastrado com sucesso.");
        return ResponseEntity.status(HttpStatus.CREATED).body(rm);
    }

    // ------------------------ ATUALIZAÇÃO ------------------------
    public ResponseEntity<?> atualizar(LivroDTO dto, MultipartFile file) {
        rm.setMensagem("");

        if (isVazio(dto.getIsbn())) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroExistente = lr.findByIsbn(dto.getIsbn());
        if (livroExistente.isEmpty()) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        preencherComGoogleBooks(dto);

        ResponseEntity<?> erroValidacao = validarCampos(dto);
        if (erroValidacao != null) return erroValidacao;

        LivroModel livro = montarLivro(dto, file);
        livro.setIsbn(dto.getIsbn()); // garante que o ISBN não muda

        lr.save(livro);
        rm.setMensagem("Livro atualizado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // ------------------------ MÉTODOS AUXILIARES ------------------------
    private void preencherComGoogleBooks(LivroDTO dto) {
        LivroModel livroGoogle = googleBooksService.buscarLivroPorIsbn(dto.getIsbn());
        if (livroGoogle != null) {
            if (isVazio(dto.getNome())) dto.setNome(livroGoogle.getNome());
            if (isVazio(dto.getEditora())) dto.setEditora(livroGoogle.getEditora());
            if (dto.getNumero_paginas() == null) dto.setNumero_paginas(livroGoogle.getNumero_paginas());
            if (dto.getData_lancamento() == null) dto.setData_lancamento(livroGoogle.getData_lancamento());
            if (isVazio(dto.getSinopse())) dto.setSinopse(livroGoogle.getSinopse());
            if (isVazio(dto.getImagem())) dto.setImagem(livroGoogle.getImagem());
            if (isVazio(dto.getAutor()) && livroGoogle.getAutor() != null) {
                dto.setAutor(livroGoogle.getAutor());
            }
        }
    }

    private ResponseEntity<?> validarCampos(LivroDTO dto) {
        if (isVazio(dto.getNome())) return erro("O título é obrigatório.");
        if (dto.getData_lancamento() == null) return erro("A data é obrigatória.");
        if (dto.getData_lancamento().isAfter(LocalDate.now())) return erro("A data de lançamento não pode ser no futuro.");
        if (dto.getNumero_paginas() == null || dto.getNumero_paginas() <= 0) return erro("O número de páginas é obrigatório.");
        if (isVazio(dto.getEditora())) return erro("A editora é obrigatória.");
        if (isVazio(dto.getCdd())) return erro("O CDD é obrigatório.");
        if (isVazio(dto.getAutor())) return erro("O autor é obrigatório.");
        if (isVazio(dto.getGenero())) return erro("O gênero é obrigatório.");
        return null;
    }

    private LivroModel montarLivro(LivroDTO dto, MultipartFile file) {
        LivroModel livro = new LivroModel();
        livro.setIsbn(dto.getIsbn());
        livro.setNome(dto.getNome());
        livro.setData_lancamento(dto.getData_lancamento());
        livro.setNumero_paginas(dto.getNumero_paginas());

        // Enums com segurança
        try {
            livro.setCdd(Cdd.valueOf(dto.getCdd().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("CDD inválido: " + dto.getCdd());
        }
        try {
            livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Classificação etária inválida: " + dto.getClassificacao_etaria());
        }
        try {
            livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo de capa inválido: " + dto.getTipo_capa());
        }

        livro.setEditora(dto.getEditora());
        livro.setNumero_capitulos(dto.getNumero_capitulos());
        livro.setEdicao(dto.getEdicao());
        livro.setVolume(dto.getVolume());
        livro.setQuantidade(dto.getQuantidade());
        livro.setSinopse(dto.getSinopse());
        livro.setAutor(dto.getAutor());
        livro.setGenero(dto.getGenero());

        // Imagem
        if (file != null && !file.isEmpty()) {
            try {
                String nomeArquivo = file.getOriginalFilename();
                String url = UrlUtils.gerarUrlValida(BASE_URL_CAPAS, "", nomeArquivo);
                livro.setImagem(url);
                storageService.uploadFile(file);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        } else if (!isVazio(dto.getImagem())) {
            livro.setImagem(dto.getImagem());
        }

        return livro;
    }

    private boolean isVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private ResponseEntity<?> erro(String mensagem) {
        rm.setMensagem(mensagem);
        return ResponseEntity.badRequest().body(rm);
    }

    // ------------------------ EXCLUSÃO ------------------------
    public ResponseEntity<ResponseModel> excluir(String isbn) { if (!lr.existsById(isbn)) { rm.setMensagem("Livro não encontrado."); return ResponseEntity.badRequest().body(rm); } lr.deleteById(isbn); rm.setMensagem("Livro removido com sucesso."); return ResponseEntity.ok(rm); }

    @Transactional
    public ResponseEntity<?> excluirLivroComExemplares(String isbn) {
        Optional<LivroModel> livroOpt = lr.findByIsbn(isbn);
        if (livroOpt.isEmpty()) return erro("Livro não encontrado.");

        er.deleteAllByLivroIsbn(isbn);
        lr.delete(livroOpt.get());

        rm.setMensagem("Livro e todos os exemplares foram removidos com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(String isbn) {
        Long quantidade = er.contarExemplaresPorLivro(isbn);
        lr.findById(isbn).ifPresent(l -> {
            l.setQuantidade(quantidade.intValue());
            lr.save(l);
        });
    }
}
