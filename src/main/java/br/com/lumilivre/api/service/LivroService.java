package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.genero.GeneroCatalogoResponse;
import br.com.lumilivre.api.dto.livro.LivroRequest;
import br.com.lumilivre.api.dto.livro.LivroListagemResponse;
import br.com.lumilivre.api.dto.livro.LivroListagemProjection;
import br.com.lumilivre.api.dto.livro.LivroAgrupadoResponse;
import br.com.lumilivre.api.dto.livro.LivroDetalheResponse;
import br.com.lumilivre.api.dto.livro.LivroResponse;
import br.com.lumilivre.api.dto.livro.LivroMobileResponse;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.CddModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.repository.CddRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.GeneroRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.service.infra.GoogleBooksService;
import br.com.lumilivre.api.service.infra.BrasilApiService;
import br.com.lumilivre.api.service.infra.SupabaseStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    private final BrasilApiService brasilApiService;
    private final GeneroRepository generoRepository;
    private final CddRepository cddRepository;

    @Value("${supabase.storage.base-url-capas}")
    private String BASE_URL_CAPAS;

    public LivroService(ExemplarRepository er, LivroRepository lr, SupabaseStorageService storageService,
            GoogleBooksService googleBooksService, GeneroRepository gr, CddRepository cddRepository,
            BrasilApiService brasilApiService) {
        this.exemplarRepository = er;
        this.livroRepository = lr;
        this.storageService = storageService;
        this.googleBooksService = googleBooksService;
        this.generoRepository = gr;
        this.cddRepository = cddRepository;
        this.brasilApiService = brasilApiService;
    }

    // ------------------------ BUSCAS ------------------------

    public List<LivroListagemResponse> buscarTodos() {
        return livroRepository.findAllCompleto().stream()
                .map(this::converterParaListaDTO)
                .collect(Collectors.toList());
    }

    public Page<LivroMobileResponse> buscarMobilePorTexto(String texto, Pageable pageable) {
        return livroRepository.buscarMobilePorTexto(texto, pageable);
    }

    public Page<LivroAgrupadoResponse> buscarAvancado(
            String nome, String isbn, String autor, String genero, String editora,
            String cdd, String classificacaoEtariaStr, String tipoCapaStr, LocalDate dataLancamento,
            Pageable pageable) {

        ClassificacaoEtaria classificacao = null;
        if (classificacaoEtariaStr != null && !classificacaoEtariaStr.isBlank()) {
            try {
                classificacao = ClassificacaoEtaria.valueOf(classificacaoEtariaStr);
            } catch (IllegalArgumentException e) {
            }
        }

        TipoCapa tipoCapa = null;
        if (tipoCapaStr != null && !tipoCapaStr.isBlank()) {
            try {
                tipoCapa = TipoCapa.valueOf(tipoCapaStr);
            } catch (IllegalArgumentException e) {
            }
        }

        String nomeFiltro = prepararFiltroLike(nome);
        String autorFiltro = prepararFiltroLike(autor);
        String generoFiltro = prepararFiltroLike(genero);
        String editoraFiltro = prepararFiltroLike(editora);
        String isbnFiltro = tratarString(isbn);
        String cddFiltro = tratarString(cdd);

        return livroRepository.buscarAvancado(
                nomeFiltro, isbnFiltro, autorFiltro, generoFiltro, editoraFiltro,
                cddFiltro, classificacao, tipoCapa, dataLancamento,
                pageable);
    }

    public Page<LivroListagemResponse> buscarParaListaAdmin(Pageable pageable) {
        Page<LivroListagemProjection> projecoes = livroRepository.findLivrosParaListaAdmin(pageable);
        return projecoes.map(p -> new LivroListagemResponse(
                StatusLivro.valueOf(p.getStatus()),
                p.getTomboExemplar(),
                p.getIsbn(),
                p.getCdd(),
                p.getNome(),
                p.getGenero(),
                p.getAutor(),
                p.getEditora(),
                p.getLocalizacao_fisica()));
    }

    public Page<LivroAgrupadoResponse> buscarLivrosAgrupados(Pageable pageable, String texto) {
        return livroRepository.findLivrosAgrupados(pageable, texto);
    }

    @Cacheable(value = "livro-detalhe", key = "#id")
    public Optional<LivroDetalheResponse> findById(Long id) {
        log.info("Buscando livro ID {} no banco de dados (sem cache)...", id);
        return livroRepository.findByIdWithDetails(id).map(livro -> {
            long disponiveis = exemplarRepository.countExemplaresByStatus(id, StatusLivro.DISPONIVEL);
            long total = exemplarRepository.countByLivroId(id);
            return new LivroDetalheResponse(livro, disponiveis, total);
        });
    }

    @Cacheable("catalogo-mobile")
    public List<GeneroCatalogoResponse> buscarCatalogoParaMobile() {
        log.info("Buscando catálogo mobile no banco de dados (sem cache)...");
        List<Map<String, Object>> results = livroRepository.findCatalogoMobile();

        Map<String, List<LivroMobileResponse>> livrosPorGenero = results.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get("genero_nome"),
                        Collectors.mapping(row -> new LivroMobileResponse(
                                ((Number) row.get("id")).longValue(),
                                (String) row.get("imagem"),
                                (String) row.get("nome"),
                                (String) row.get("autor"),
                                row.get("avaliacao") != null ? ((Number) row.get("avaliacao")).doubleValue() : 4.6),
                                Collectors.toList())));

        return livrosPorGenero.entrySet().stream()
                .map(entry -> new GeneroCatalogoResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(GeneroCatalogoResponse::getNome))
                .collect(Collectors.toList());
    }

    public Page<LivroMobileResponse> buscarPorGenero(String nomeGenero, Pageable pageable) {
        return livroRepository.findByGeneroAsCatalogoDTO(nomeGenero, pageable);
    }

    public Page<LivroListagemResponse> buscarPorTexto(String texto, Pageable pageable) {
        Page<LivroModel> paginaDeLivros = livroRepository.findIdsPorTexto(texto, pageable);
        List<LivroModel> livrosComGeneros = livroRepository.findWithGeneros(paginaDeLivros.getContent());

        List<LivroListagemResponse> dtos = livrosComGeneros.stream()
                .map(this::converterParaListaDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, paginaDeLivros.getTotalElements());
    }

    @Cacheable(value = "contagem_livros")
    public long getContagemLivros() {
        return livroRepository.count();
    }

    // ------------------------ UPLOAD DE CAPA ------------------------

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "catalogo-mobile", allEntries = true),
    })
    public void uploadCapa(Long id, MultipartFile file) {
        LivroModel livro = livroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado para o ID: " + id));

        if (file != null && !file.isEmpty()) {
            try {
                String url = storageService.uploadFile(file, "capas");
                livro.setImagem(url);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        }
    }

    // ------------------------ CADASTRO ------------------------

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "catalogo-mobile", allEntries = true),
            @CacheEvict(value = "contagem_livros", allEntries = true)
    })
    public LivroResponse cadastrar(LivroRequest dto, MultipartFile file) {
        if (isNaoVazio(dto.getIsbn()) && livroRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            throw new RegraDeNegocioException("Esse ISBN já está cadastrado em outro livro.");
        }

        if (isNaoVazio(dto.getIsbn())) {
            preencherDadosExternos(dto);
        }

        validarCampos(dto);

        return salvarLivroNoBanco(dto, file);
    }

    @Transactional
    protected LivroResponse salvarLivroNoBanco(LivroRequest dto, MultipartFile file) {
        try {
            LivroModel livro = montarLivro(new LivroModel(), dto, file);
            LivroModel salvo = livroRepository.save(livro);
            return new LivroResponse(salvo);
        } catch (Exception e) {
            log.error("Erro ao montar ou salvar o livro: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao cadastrar o livro: " + e.getMessage());
        }
    }

    // ------------------------ ATUALIZAÇÃO ------------------------

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "catalogo-mobile", allEntries = true),
    })
    public LivroResponse atualizar(Long id, LivroRequest dto, MultipartFile file) {
        LivroModel livroParaAtualizar = livroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado para o ID: " + id));

        if (isNaoVazio(dto.getIsbn())) {
            Optional<LivroModel> livroComMesmoIsbn = livroRepository.findByIsbn(dto.getIsbn());
            if (livroComMesmoIsbn.isPresent() && !livroComMesmoIsbn.get().getId().equals(id)) {
                throw new RegraDeNegocioException("O ISBN informado já pertence a outro livro.");
            }
        } else {
            dto.setIsbn(livroParaAtualizar.getIsbn());
        }

        if (isNaoVazio(dto.getIsbn())) {
            preencherDadosExternos(dto);
        }

        validarCampos(dto);

        try {
            LivroModel livroAtualizado = montarLivro(livroParaAtualizar, dto, file);
            LivroModel salvo = livroRepository.save(livroAtualizado);
            return new LivroResponse(salvo);

        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao montar ou atualizar o livro ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao atualizar o livro: " + e.getMessage());
        }
    }

    // ------------------------ EXCLUSÃO ------------------------

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "catalogo-mobile", allEntries = true),
    })
    public void excluirLivroComExemplares(Long id) {
        if (!livroRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Livro não encontrado.");
        }
        exemplarRepository.deleteAllByLivroId(id);
        livroRepository.deleteById(id);
    }

    // ------------------------ MÉTODOS AUXILIARES ------------------------

    private LivroListagemResponse converterParaListaDTO(LivroModel l) {
        String generos = l.getGeneros().stream().map(GeneroModel::getNome).collect(Collectors.joining(", "));
        return new LivroListagemResponse(
                StatusLivro.DISPONIVEL,
                "N/A",
                l.getIsbn(),
                l.getCdd() != null ? l.getCdd().getCodigo() : "",
                l.getNome(),
                generos,
                l.getAutor(),
                l.getEditora(),
                "Ver Exemplares");
    }

    private void preencherDadosExternos(LivroRequest dto) {
        boolean googleEncontrou = false;
        boolean temCapa = isNaoVazio(dto.getImagem());

        try {
            var googleOpt = googleBooksService.buscarLivroInteligente(dto.getIsbn(), dto.getNome(), dto.getAutor());

            if (googleOpt.isPresent()) {
                googleEncontrou = true;
                var livroGoogle = googleOpt.get().livro();
                var googleData = googleOpt.get();

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
                if (isVazio(dto.getAutor()) && isNaoVazio(livroGoogle.getAutor()))
                    dto.setAutor(livroGoogle.getAutor());

                if (!temCapa && isNaoVazio(livroGoogle.getImagem())) {
                    dto.setImagem(livroGoogle.getImagem());
                    temCapa = true;
                }

                if (googleData.averageRating() != null) {
                    dto.setAvaliacao(googleData.averageRating());
                } else if (dto.getAvaliacao() == null) {
                    dto.setAvaliacao(4.6);
                }
            }
        } catch (Exception e) {
            log.warn("Falha na busca Google Books: {}", e.getMessage());
        }

        if ((!googleEncontrou || !temCapa) && isNaoVazio(dto.getIsbn())) {
            try {
                var brasilOpt = brasilApiService.buscarPorIsbn(dto.getIsbn());

                if (brasilOpt.isPresent()) {
                    var brData = brasilOpt.get();
                    log.info("Dados encontrados na BrasilAPI para ISBN {}", dto.getIsbn());

                    if (isVazio(dto.getNome()))
                        dto.setNome(brData.title());
                    if (isVazio(dto.getEditora()))
                        dto.setEditora(brData.publisher());
                    if (isVazio(dto.getSinopse()))
                        dto.setSinopse(brData.synopsis());

                    if (dto.getNumero_paginas() == null || dto.getNumero_paginas() == 0) {
                        dto.setNumero_paginas(brData.pageCount());
                    }

                    if (dto.getData_lancamento() == null && brData.year() != null) {
                        dto.setData_lancamento(LocalDate.of(brData.year(), 1, 1));
                    }

                    if (isVazio(dto.getAutor()) && brData.authors() != null && !brData.authors().isEmpty()) {
                        dto.setAutor(String.join(", ", brData.authors()));
                    }

                    if (!temCapa && isNaoVazio(brData.coverUrl())) {
                        dto.setImagem(brData.coverUrl());
                    }
                }
            } catch (Exception e) {
                log.warn("Falha no fallback BrasilAPI: {}", e.getMessage());
            }
        }

        // Valor default de avaliação se nada encontrou
        if (dto.getAvaliacao() == null) {
            dto.setAvaliacao(4.6);
        }
    }

    private void validarCampos(LivroRequest dto) {
        if (isVazio(dto.getNome()))
            throw new RegraDeNegocioException("O título é obrigatório.");
        if (dto.getData_lancamento() != null && dto.getData_lancamento().isAfter(LocalDate.now()))
            throw new RegraDeNegocioException("A data de lançamento não pode ser no futuro.");
        if (isVazio(dto.getEditora()))
            throw new RegraDeNegocioException("A editora é obrigatória.");
        if (isVazio(dto.getAutor()))
            throw new RegraDeNegocioException("O autor é obrigatório.");
    }

    private LivroModel montarLivro(LivroModel livro, LivroRequest dto, MultipartFile file) {
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
        livro.setAvaliacao(dto.getAvaliacao() != null ? dto.getAvaliacao() : 4.6);

        if (isNaoVazio(dto.getCdd())) {
            CddModel cdd = cddRepository.findById(dto.getCdd())
                    .orElseThrow(() -> new RegraDeNegocioException("Código CDD inválido: " + dto.getCdd()));
            livro.setCdd(cdd);
        } else {
            livro.setCdd(null);
        }

        Set<GeneroModel> generos = new HashSet<>();

        if (dto.getGeneros() != null && !dto.getGeneros().isEmpty()) {
            Set<GeneroModel> generosEncontrados = generoRepository.findByNomeIn(dto.getGeneros());

            generos = generosEncontrados.stream()
                    .limit(3)
                    .collect(Collectors.toSet());
        }

        livro.setGeneros(generos);

        try {
            if (isNaoVazio(dto.getClassificacao_etaria())) {
                livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
            }

            if (isNaoVazio(dto.getTipo_capa())) {
                livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
            } else {
                livro.setTipo_capa(null);
            }

        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException(
                    "Classificação etária ou Tipo de capa inválido: Verifique os valores enviados.");
        }

        if (file != null && !file.isEmpty()) {
            try {
                String url = storageService.uploadFile(file, "capas");
                livro.setImagem(url);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        } else if (isNaoVazio(dto.getImagem())) {
            livro.setImagem(dto.getImagem());
        }

        return livro;
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

    private String prepararFiltroLike(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return "%" + valor.trim().toLowerCase() + "%";
    }

    private String tratarString(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return valor.trim();
    }

    public LivroRequest pesquisarDadosPorIsbn(String isbn) {
        LivroRequest dto = new LivroRequest();
        dto.setIsbn(isbn);

        preencherDadosExternos(dto);

        if (isVazio(dto.getNome())) {
            throw new RecursoNaoEncontradoException("Livro não encontrado nas bases externas para o ISBN: " + isbn);
        }

        return dto;
    }
}