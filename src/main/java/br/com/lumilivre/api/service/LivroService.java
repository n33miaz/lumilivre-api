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
import java.time.LocalDateTime;
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

    public List<LivroListagemResponse> buscarTodos() {
        return livroRepository.findAll().stream()
                .map(this::converterParaListaDTO)
                .collect(Collectors.toList());
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

        LocalDateTime dataLancamentoTime = (dataLancamento != null) ? dataLancamento.atStartOfDay() : null;

        return livroRepository.buscarAvancado(
                nome, isbn, autor, genero, editora,
                cdd, classificacao, tipoCapa, dataLancamentoTime,
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
                                (String) row.get("autor")), Collectors.toList())));

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

    // ------------------------ UPLOAD DE CAPA ------------------------

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "livro-detalhe", key = "#id"),
            @CacheEvict(value = "catalogo-mobile", allEntries = true)
    })
    public void uploadCapa(Long id, MultipartFile file) {
        LivroModel livro = livroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado para o ID: " + id));

        try {
            String url = storageService.uploadFile(file, "livros");
            livro.setImagem(url);
            livroRepository.save(livro);
        } catch (Exception e) {
            log.error("Erro ao fazer upload da capa para o livro ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Erro ao fazer upload da capa: " + e.getMessage());
        }
    }

    // ------------------------ CADASTRO ------------------------

    @Transactional
    @CacheEvict(value = "catalogo-mobile", allEntries = true)
    public LivroResponse cadastrar(LivroRequest dto, MultipartFile file) {
        if (isNaoVazio(dto.getIsbn()) && livroRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            throw new RegraDeNegocioException("Esse ISBN já está cadastrado em outro livro.");
        }

        if (isNaoVazio(dto.getIsbn())) {
            preencherComGoogleBooks(dto);
        }

        validarCampos(dto);

        try {
            LivroModel livro = montarLivro(new LivroModel(), dto, file);
            LivroModel salvo = livroRepository.save(livro);
            return new LivroResponse(salvo);

        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao montar ou salvar o livro: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao cadastrar o livro: " + e.getMessage());
        }
    }

    // ------------------------ ATUALIZAÇÃO ------------------------

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "livro-detalhe", key = "#id"),
            @CacheEvict(value = "catalogo-mobile", allEntries = true)
    })
    public LivroResponse atualizar(Long id, LivroRequest dto, MultipartFile file) {
        LivroModel livroParaAtualizar = livroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado para o ID: " + id));

        if (isNaoVazio(dto.getIsbn())) {
            Optional<LivroModel> livroComMesmoIsbn = livroRepository.findByIsbn(dto.getIsbn());
            if (livroComMesmoIsbn.isPresent() && !livroComMesmoIsbn.get().getId().equals(id)) {
                throw new RegraDeNegocioException("O ISBN informado já pertence a outro livro.");
            }
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
            @CacheEvict(value = "livro-detalhe", key = "#id"),
            @CacheEvict(value = "catalogo-mobile", allEntries = true)
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

    private void preencherComGoogleBooks(LivroRequest dto) {
        try {
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
        } catch (Exception e) {
            log.warn("Falha ao buscar dados no Google Books, prosseguindo com cadastro manual: {}", e.getMessage());
        }
    }

    private void validarCampos(LivroRequest dto) {
        if (isVazio(dto.getNome()))
            throw new RegraDeNegocioException("O título é obrigatório.");
        if (dto.getData_lancamento() == null)
            throw new RegraDeNegocioException("A data de lançamento é obrigatória.");
        if (dto.getData_lancamento().isAfter(LocalDate.now()))
            throw new RegraDeNegocioException("A data de lançamento não pode ser no futuro.");
        if (dto.getNumero_paginas() == null || dto.getNumero_paginas() <= 0)
            throw new RegraDeNegocioException("O número de páginas é obrigatório e deve ser maior que zero.");
        if (isVazio(dto.getEditora()))
            throw new RegraDeNegocioException("A editora é obrigatória.");
        if (isVazio(dto.getCdd()))
            throw new RegraDeNegocioException("O CDD é obrigatório.");
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

        CddModel cdd = cddRepository.findById(dto.getCdd())
                .orElseThrow(() -> new RegraDeNegocioException("Código CDD inválido: " + dto.getCdd()));
        livro.setCdd(cdd);

        try {
            livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
            livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        } catch (Exception e) {
            throw new RegraDeNegocioException("Classificação etária ou Tipo de capa inválido.");
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
}