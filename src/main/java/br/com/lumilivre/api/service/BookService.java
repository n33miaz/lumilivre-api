package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.BOOK_COUNT;
import static br.com.lumilivre.api.config.CacheNames.BOOK_DETAIL;
import static br.com.lumilivre.api.config.CacheNames.MOBILE_CATALOG;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

import br.com.lumilivre.api.dto.genero.GeneroCatalogoResponse;
import br.com.lumilivre.api.dto.livro.LivroAgrupadoResponse;
import br.com.lumilivre.api.dto.livro.LivroDetalheResponse;
import br.com.lumilivre.api.dto.livro.LivroListagemProjection;
import br.com.lumilivre.api.dto.livro.LivroListagemResponse;
import br.com.lumilivre.api.dto.livro.LivroMobileResponse;
import br.com.lumilivre.api.dto.livro.LivroRequest;
import br.com.lumilivre.api.dto.livro.LivroResponse;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.DeweyClassification;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import br.com.lumilivre.api.repository.GenreRepository;
import br.com.lumilivre.api.service.infra.BrasilApiService;
import br.com.lumilivre.api.service.infra.GoogleBooksService;
import br.com.lumilivre.api.service.infra.SupabaseStorageService;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final SupabaseStorageService storageService;
    private final GoogleBooksService googleBooksService;
    private final BrasilApiService brasilApiService;
    private final GenreRepository genreRepository;
    private final DeweyClassificationRepository deweyClassificationRepository;

    @Value("${supabase.storage.base-url-capas}")
    private String baseUrlCapas;

    public BookService(BookCopyRepository bookCopyRepository, BookRepository bookRepository,
            SupabaseStorageService storageService, GoogleBooksService googleBooksService,
            GenreRepository genreRepository, DeweyClassificationRepository deweyClassificationRepository,
            BrasilApiService brasilApiService) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.storageService = storageService;
        this.googleBooksService = googleBooksService;
        this.genreRepository = genreRepository;
        this.deweyClassificationRepository = deweyClassificationRepository;
        this.brasilApiService = brasilApiService;
    }

    public List<LivroListagemResponse> buscarTodos() {
        return bookRepository.findAllCompleto().stream()
                .map(this::converterParaListaDTO)
                .collect(Collectors.toList());
    }

    public Page<LivroMobileResponse> buscarMobilePorTexto(String texto, Pageable pageable) {
        return bookRepository.buscarMobilePorTexto(texto, pageable);
    }

    public Page<LivroAgrupadoResponse> buscarAvancado(
            String nome, String isbn, String autor, String genero, String editora,
            String cdd, String classificacaoEtariaStr, String tipoCapaStr, LocalDate dataLancamento,
            Pageable pageable) {

        AgeRating classificacao = null;
        if (classificacaoEtariaStr != null && !classificacaoEtariaStr.isBlank()) {
            try {
                classificacao = AgeRating.valueOf(classificacaoEtariaStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        CoverType tipoCapa = null;
        if (tipoCapaStr != null && !tipoCapaStr.isBlank()) {
            try {
                tipoCapa = CoverType.valueOf(tipoCapaStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return bookRepository.buscarAvancado(
                prepararFiltroLike(nome), tratarString(isbn), prepararFiltroLike(autor),
                prepararFiltroLike(genero), prepararFiltroLike(editora),
                tratarString(cdd), classificacao, tipoCapa, dataLancamento, pageable);
    }

    public Page<LivroListagemResponse> buscarParaListaAdmin(Pageable pageable) {
        Page<LivroListagemProjection> projecoes = bookRepository.findLivrosParaListaAdmin(pageable);
        return projecoes.map(p -> new LivroListagemResponse(
                BookCopyStatus.valueOf(p.getStatus()),
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
        return bookRepository.findLivrosAgrupados(pageable, texto);
    }

    @Cacheable(value = BOOK_DETAIL, key = "#id")
    public Optional<LivroDetalheResponse> findById(UUID id) {
        log.info("Buscando livro ID {} no banco de dados (sem cache)...", id);
        return bookRepository.findByIdWithDetails(id).map(book -> {
            long disponiveis = bookCopyRepository.countByBookIdAndStatus(id, BookCopyStatus.AVAILABLE);
            long total = bookCopyRepository.countByBook_Id(id);
            return new LivroDetalheResponse(book, disponiveis, total);
        });
    }

    @Cacheable(MOBILE_CATALOG)
    public List<GeneroCatalogoResponse> buscarCatalogoParaMobile() {
        log.info("Buscando catálogo mobile no banco de dados (sem cache)...");
        List<Map<String, Object>> results = bookRepository.findCatalogoMobile();

        Map<String, List<LivroMobileResponse>> livrosPorGenero = results.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get("genero_nome"),
                        Collectors.mapping(row -> new LivroMobileResponse(
                                UUID.fromString(row.get("id").toString()),
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
        return bookRepository.findByGeneroAsCatalogoDTO(nomeGenero, pageable);
    }

    public Page<LivroListagemResponse> buscarPorTexto(String texto, Pageable pageable) {
        Page<Book> paginaDeLivros = bookRepository.findIdsPorTexto(texto, pageable);
        List<Book> livrosComGeneros = bookRepository.findWithGeneros(paginaDeLivros.getContent());

        List<LivroListagemResponse> dtos = livrosComGeneros.stream()
                .map(this::converterParaListaDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, paginaDeLivros.getTotalElements());
    }

    @Cacheable(value = BOOK_COUNT)
    public long getContagemLivros() {
        return bookRepository.count();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
    })
    public void uploadCapa(UUID id, MultipartFile file) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado para o ID: " + id));

        if (file != null && !file.isEmpty()) {
            try {
                String url = storageService.uploadFile(file, "capas");
                book.setCoverUrl(url);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
            @CacheEvict(value = BOOK_COUNT, allEntries = true)
    })
    public LivroResponse cadastrar(LivroRequest dto, MultipartFile file) {
        if (isNaoVazio(dto.getIsbn()) && bookRepository.findByIsbn(dto.getIsbn()).isPresent()) {
            throw new BusinessRuleException("Esse ISBN já está cadastrado em outro livro.");
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
            Book book = montarLivro(new Book(), dto, file);
            Book savedBook = bookRepository.save(book);
            return new LivroResponse(savedBook);
        } catch (Exception e) {
            log.error("Erro ao montar ou salvar o livro: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao cadastrar o livro: " + e.getMessage());
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
    })
    public LivroResponse atualizar(UUID id, LivroRequest dto, MultipartFile file) {
        Book bookToUpdate = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado para o ID: " + id));

        if (isNaoVazio(dto.getIsbn())) {
            Optional<Book> livroComMesmoIsbn = bookRepository.findByIsbn(dto.getIsbn());
            if (livroComMesmoIsbn.isPresent() && !livroComMesmoIsbn.get().getId().equals(id)) {
                throw new BusinessRuleException("O ISBN informado já pertence a outro livro.");
            }
        } else {
            dto.setIsbn(bookToUpdate.getIsbn());
        }

        if (isNaoVazio(dto.getIsbn())) {
            preencherDadosExternos(dto);
        }

        validarCampos(dto);

        try {
            Book updatedBook = montarLivro(bookToUpdate, dto, file);
            Book savedBook = bookRepository.save(updatedBook);
            return new LivroResponse(savedBook);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao montar ou atualizar o livro ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao atualizar o livro: " + e.getMessage());
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
    })
    public void excluirLivroComExemplares(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Livro não encontrado.");
        }
        bookCopyRepository.deleteAllByBook_Id(id);
        bookRepository.deleteById(id);
    }

    private LivroListagemResponse converterParaListaDTO(Book book) {
        String genres = book.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "));
        return new LivroListagemResponse(
                BookCopyStatus.AVAILABLE,
                "N/A",
                book.getIsbn(),
                book.getDeweyClassification() != null ? book.getDeweyClassification().getCode() : "",
                book.getTitle(),
                genres,
                book.getAuthor(),
                book.getPublisher(),
                "Ver Exemplares");
    }

    private void preencherDadosExternos(LivroRequest dto) {
        boolean googleEncontrou = false;
        boolean temCapa = isNaoVazio(dto.getImagem());

        try {
            var googleOpt = googleBooksService.findBestBookMatch(dto.getIsbn(), dto.getNome(), dto.getAutor());

            if (googleOpt.isPresent()) {
                googleEncontrou = true;
                var livroGoogle = googleOpt.get().livro();
                var googleData = googleOpt.get();

                if (isVazio(dto.getNome())) dto.setNome(livroGoogle.getTitle());
                if (isVazio(dto.getEditora())) dto.setEditora(livroGoogle.getPublisher());
                if (dto.getNumero_paginas() == null || dto.getNumero_paginas() == 0)
                    dto.setNumero_paginas(livroGoogle.getPageCount());
                if (dto.getData_lancamento() == null) dto.setData_lancamento(livroGoogle.getPublicationDate());
                if (isVazio(dto.getSinopse())) dto.setSinopse(livroGoogle.getSynopsis());
                if (isVazio(dto.getAutor()) && isNaoVazio(livroGoogle.getAuthor()))
                    dto.setAutor(livroGoogle.getAuthor());

                if (!temCapa && isNaoVazio(livroGoogle.getCoverUrl())) {
                    dto.setImagem(livroGoogle.getCoverUrl());
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
                    if (isVazio(dto.getNome())) dto.setNome(brData.title());
                    if (isVazio(dto.getEditora())) dto.setEditora(brData.publisher());
                    if (isVazio(dto.getSinopse())) dto.setSinopse(brData.synopsis());
                    if (dto.getNumero_paginas() == null || dto.getNumero_paginas() == 0)
                        dto.setNumero_paginas(brData.pageCount());
                    if (dto.getData_lancamento() == null && brData.year() != null)
                        dto.setData_lancamento(LocalDate.of(brData.year(), 1, 1));
                    if (isVazio(dto.getAutor()) && brData.authors() != null && !brData.authors().isEmpty())
                        dto.setAutor(String.join(", ", brData.authors()));
                    if (!temCapa && isNaoVazio(brData.coverUrl()))
                        dto.setImagem(brData.coverUrl());
                }
            } catch (Exception e) {
                log.warn("Falha no fallback BrasilAPI: {}", e.getMessage());
            }
        }

        if (dto.getAvaliacao() == null) dto.setAvaliacao(4.6);
    }

    private void validarCampos(LivroRequest dto) {
        if (isVazio(dto.getNome())) throw new BusinessRuleException("O título é obrigatório.");
        if (dto.getData_lancamento() != null && dto.getData_lancamento().isAfter(LocalDate.now()))
            throw new BusinessRuleException("A data de lançamento não pode ser no futuro.");
        if (isVazio(dto.getEditora())) throw new BusinessRuleException("A editora é obrigatória.");
        if (isVazio(dto.getAutor())) throw new BusinessRuleException("O autor é obrigatório.");
    }

    private Book montarLivro(Book book, LivroRequest dto, MultipartFile file) {
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getNome());
        book.setPublicationDate(dto.getData_lancamento());
        book.setPageCount(dto.getNumero_paginas());
        book.setPublisher(dto.getEditora());
        book.setEdition(dto.getEdicao());
        book.setVolume(dto.getVolume());
        book.setSynopsis(dto.getSinopse());
        book.setAuthor(dto.getAutor());
        book.setRating(dto.getAvaliacao() != null ? dto.getAvaliacao() : 4.6);

        if (isNaoVazio(dto.getCdd())) {
            DeweyClassification cdd = deweyClassificationRepository.findById(dto.getCdd())
                    .orElseThrow(() -> new BusinessRuleException("Código CDD inválido: " + dto.getCdd()));
            book.setDeweyClassification(cdd);
        } else {
            book.setDeweyClassification(null);
        }

        Set<Genre> genres = new HashSet<>();
        if (dto.getGeneros() != null && !dto.getGeneros().isEmpty()) {
            genres = genreRepository.findByNameIn(dto.getGeneros()).stream()
                    .limit(3)
                    .collect(Collectors.toSet());
        }
        book.setGenres(genres);

        try {
            if (isNaoVazio(dto.getClassificacao_etaria())) {
                book.setAgeRating(AgeRating.valueOf(dto.getClassificacao_etaria().toUpperCase()));
            }
            book.setCoverType(isNaoVazio(dto.getTipo_capa())
                    ? CoverType.valueOf(dto.getTipo_capa().toUpperCase())
                    : null);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(
                    "Classificação etária ou Tipo de capa inválido: Verifique os valores enviados.");
        }

        if (file != null && !file.isEmpty()) {
            try {
                book.setCoverUrl(storageService.uploadFile(file, "capas"));
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        } else if (isNaoVazio(dto.getImagem())) {
            book.setCoverUrl(dto.getImagem());
        }

        return book;
    }

    public LivroRequest pesquisarDadosPorIsbn(String isbn) {
        LivroRequest dto = new LivroRequest();
        dto.setIsbn(isbn);
        preencherDadosExternos(dto);
        if (isVazio(dto.getNome())) {
            throw new ResourceNotFoundException("Livro não encontrado nas bases externas para o ISBN: " + isbn);
        }
        return dto;
    }

    private boolean isVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private boolean isNaoVazio(String valor) {
        return !isVazio(valor);
    }

    private String prepararFiltroLike(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        return "%" + valor.trim().toLowerCase() + "%";
    }

    private String tratarString(String valor) {
        if (valor == null || valor.trim().isEmpty()) return null;
        return valor.trim();
    }
}
