package br.com.lumilivre.api.service;

import br.com.lumilivre.api.data.*;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.CddModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CddRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.GeneroRepository;
import br.com.lumilivre.api.repository.LivroRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LivroService {

    private static final Logger log = LoggerFactory.getLogger(LivroService.class);

    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;
    private final SupabaseStorageService storageService;
    private final GoogleBooksService googleBooksService;
    private final GeneroRepository generoRepository;
    private final CddRepository cddRepository;

    @Value("${supabase.storage.base-url-capas}")
    private String BASE_URL_CAPAS;

    public LivroService(ExemplarRepository er, LivroRepository lr, SupabaseStorageService storageService,
            GoogleBooksService googleBooksService, GeneroRepository gr, CddRepository cddRepository) {
        this.exemplarRepository = er;
        this.livroRepository = lr;
        this.storageService = storageService;
        this.googleBooksService = googleBooksService;
        this.generoRepository = gr;
        this.cddRepository = cddRepository;
    }

    // ------------------------ BUSCAS ------------------------

    public List<LivroModel> buscarTodos() {
        return livroRepository.findAll();
    }

    public Page<ListaLivroDTO> buscarParaListaAdmin(Pageable pageable) {
        return livroRepository.findLivrosParaListaAdmin(pageable);
    }

    public Page<LivroAgrupadoDTO> buscarLivrosAgrupados(Pageable pageable, String texto) {
        return livroRepository.findLivrosAgrupados(pageable, texto);
    }

    public ResponseEntity<LivroModel> findById(Long id) {
        return livroRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public List<GeneroCatalogoDTO> buscarCatalogoParaMobile() {
        List<LivroModel> livrosDisponiveis = livroRepository.findLivrosDisponiveis();
        Map<String, Set<LivroModel>> livrosPorNomeGenero = new HashMap<>();

        for (LivroModel livro : livrosDisponiveis) {
            for (GeneroModel genero : livro.getGeneros()) {
                livrosPorNomeGenero.computeIfAbsent(genero.getNome(), k -> new HashSet<>()).add(livro);
            }
        }

        return livrosPorNomeGenero.entrySet().stream()
                .map(entry -> {
                    List<LivroResponseMobileGeneroDTO> livrosDoGenero = entry.getValue().stream()
                            .limit(10)
                            .map(livro -> new LivroResponseMobileGeneroDTO(
                                    livro.getImagem(),
                                    livro.getNome(),
                                    livro.getAutor()))
                            .collect(Collectors.toList());
                    return new GeneroCatalogoDTO(entry.getKey(), livrosDoGenero);
                })
                .filter(dto -> !dto.getLivros().isEmpty())
                .sorted(Comparator.comparingInt(g -> -g.getLivros().size()))
                .collect(Collectors.toList());
    }

    public Page<LivroModel> buscarPorGenero(String nomeGenero, Pageable pageable) {
        return livroRepository.findByGeneroNomeIgnoreCase(nomeGenero, pageable);
    }

    // ------------------------ UPLOAD DE CAPA ------------------------

    @Transactional
    public ResponseEntity<ResponseModel> uploadCapa(Long id, MultipartFile file) {
        return livroRepository.findById(id).map(livro -> {
            try {
                // Corrigido para usar o método correto do SupabaseStorageService
                String url = storageService.uploadFile(file, "livros");
                livro.setImagem(url);
                livroRepository.save(livro);
                return ResponseEntity.ok(new ResponseModel("Capa atualizada com sucesso."));
            } catch (Exception e) {
                log.error("Erro ao fazer upload da capa para o livro ID {}: {}", id, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ResponseModel("Erro ao fazer upload da capa: " + e.getMessage()));
            }
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseModel("Livro não encontrado para o ID: " + id)));
    }

    // ------------------------ CADASTRO ------------------------

    @Transactional
    public ResponseEntity<ResponseModel> cadastrar(LivroDTO dto, MultipartFile file) {
        if (isNaoVazio(dto.getIsbn()) && livroRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            return erro("Esse ISBN já está cadastrado em outro livro.");
        }

        if (isNaoVazio(dto.getIsbn())) {
            preencherComGoogleBooks(dto);
        }

        Optional<ResponseEntity<ResponseModel>> erroValidacao = validarCampos(dto);
        if (erroValidacao.isPresent())
            return erroValidacao.get();

        try {
            LivroModel livro = montarLivro(new LivroModel(), dto, file);
            livroRepository.save(livro);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseModel("Livro cadastrado com sucesso."));
        } catch (Exception e) {
            log.error("Erro ao montar ou salvar o livro: {}", e.getMessage(), e);
            return erro("Erro interno ao cadastrar o livro: " + e.getMessage());
        }
    }

    // ------------------------ ATUALIZAÇÃO ------------------------

    @Transactional
    public ResponseEntity<ResponseModel> atualizar(Long id, LivroDTO dto, MultipartFile file) {
        Optional<LivroModel> livroExistenteOpt = livroRepository.findById(id);
        if (livroExistenteOpt.isEmpty()) {
            return erro("Livro não encontrado para o ID: " + id);
        }

        if (isNaoVazio(dto.getIsbn())) {
            Optional<LivroModel> livroComMesmoIsbn = livroRepository.findByIsbn(dto.getIsbn());
            if (livroComMesmoIsbn.isPresent() && !livroComMesmoIsbn.get().getId().equals(id)) {
                return erro("O ISBN informado já pertence a outro livro.");
            }
        }

        Optional<ResponseEntity<ResponseModel>> erroValidacao = validarCampos(dto);
        if (erroValidacao.isPresent())
            return erroValidacao.get();

        try {
            LivroModel livroParaAtualizar = livroExistenteOpt.get();
            LivroModel livroAtualizado = montarLivro(livroParaAtualizar, dto, file);
            livroRepository.save(livroAtualizado);
            return ResponseEntity.ok(new ResponseModel("Livro atualizado com sucesso."));
        } catch (Exception e) {
            log.error("Erro ao montar ou atualizar o livro ID {}: {}", id, e.getMessage(), e);
            return erro("Erro interno ao atualizar o livro: " + e.getMessage());
        }
    }

    // ------------------------ EXCLUSÃO ------------------------

    @Transactional
    public ResponseEntity<ResponseModel> excluirLivroComExemplares(Long id) {
        if (!livroRepository.existsById(id)) {
            return erro("Livro não encontrado.");
        }
        exemplarRepository.deleteAllByLivroId(id);
        livroRepository.deleteById(id);
        return ResponseEntity.ok(new ResponseModel("Livro e todos os exemplares foram removidos com sucesso."));
    }

    // ------------------------ MÉTODOS AUXILIARES ------------------------

    private void preencherComGoogleBooks(LivroDTO dto) {
        googleBooksService.buscarDadosPorIsbn(dto.getIsbn()).ifPresent(googleData -> {
            LivroModel livroGoogle = googleData.livro();
            if (isVazio(dto.getNome()))
                dto.setNome(livroGoogle.getNome());
            if (isVazio(dto.getEditora()))
                dto.setEditora(livroGoogle.getEditora());
            if (dto.getNumero_paginas() == null || dto.getNumero_paginas() == 0)
                dto.setNumero_paginas(livroGoogle.getNumero_paginas());
            if (dto.getData_lancamento() == null)
                dto.setData_lancamento(livroGoogle.getData_lancamento());
            if (isVazio(dto.getSinopse()))
                dto.setSinopse(livroGoogle.getSinopse());
            if (isVazio(dto.getImagem()))
                dto.setImagem(livroGoogle.getImagem());
            if (isVazio(dto.getAutor()) && isNaoVazio(livroGoogle.getAutor()))
                dto.setAutor(livroGoogle.getAutor());
        });
    }

    private Optional<ResponseEntity<ResponseModel>> validarCampos(LivroDTO dto) {
        if (isVazio(dto.getNome()))
            return Optional.of(erro("O título é obrigatório."));
        if (dto.getData_lancamento() == null)
            return Optional.of(erro("A data de lançamento é obrigatória."));
        if (dto.getData_lancamento().isAfter(LocalDate.now()))
            return Optional.of(erro("A data de lançamento não pode ser no futuro."));
        if (dto.getNumero_paginas() == null || dto.getNumero_paginas() <= 0)
            return Optional.of(erro("O número de páginas é obrigatório e deve ser maior que zero."));
        if (isVazio(dto.getEditora()))
            return Optional.of(erro("A editora é obrigatória."));
        if (isVazio(dto.getCdd()))
            return Optional.of(erro("O CDD é obrigatório."));
        if (isVazio(dto.getAutor()))
            return Optional.of(erro("O autor é obrigatório."));
        return Optional.empty();
    }

    private LivroModel montarLivro(LivroModel livro, LivroDTO dto, MultipartFile file) {
        livro.setIsbn(dto.getIsbn());
        livro.setNome(dto.getNome());
        livro.setData_lancamento(dto.getData_lancamento());
        livro.setNumero_paginas(dto.getNumero_paginas());
        livro.setEditora(dto.getEditora());
        livro.setEdicao(dto.getEdicao());
        livro.setVolume(dto.getVolume());
        livro.setQuantidade(dto.getQuantidade());
        livro.setSinopse(dto.getSinopse());
        livro.setAutor(dto.getAutor());

        CddModel cdd = cddRepository.findById(dto.getCdd())
                .orElseThrow(() -> new IllegalArgumentException("Código CDD inválido: " + dto.getCdd()));
        livro.setCdd(cdd);

        try {
            livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
            livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Classificação etária ou Tipo de capa inválido.", e);
        }

        Set<GeneroModel> generos = processarGeneros(dto.getCdd());
        livro.setGeneros(generos);

        if (file != null && !file.isEmpty()) {
            try {
                String url = storageService.uploadFile(file, "livros");
                livro.setImagem(url);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        } else if (isNaoVazio(dto.getImagem())) {
            livro.setImagem(dto.getImagem());
        }

        return livro;
    }

    private Set<GeneroModel> processarGeneros(String cddCodigo) {
        if (cddCodigo == null || cddCodigo.isBlank()) {
            return Collections.emptySet();
        }
        return generoRepository.findAllByCddCodigo(cddCodigo);
    }

    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(Long livroId) {
        if (livroId == null)
            return;
        long quantidade = exemplarRepository.countByLivroId(livroId);
        livroRepository.findById(livroId).ifPresent(livro -> {
            livro.setQuantidade((int) quantidade);
            livroRepository.save(livro);
        });
    }

    private boolean isVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private boolean isNaoVazio(String valor) {
        return !isVazio(valor);
    }

    private ResponseEntity<ResponseModel> erro(String mensagem) {
        return ResponseEntity.badRequest().body(new ResponseModel(mensagem));
    }
}