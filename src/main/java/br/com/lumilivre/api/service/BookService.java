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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.dto.book.BookCatalogResponse;
import br.com.lumilivre.api.dto.book.BookGroupedResponse;
import br.com.lumilivre.api.dto.book.BookListItemProjection;
import br.com.lumilivre.api.dto.book.BookRequest;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
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

    public Page<BookCardResponse> buscarMobilePorTexto(String texto, Pageable pageable) {
        return bookRepository.buscarMobilePorTexto(texto, pageable);
    }

    public Page<BookGroupedResponse> buscarAvancado(
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

    public Page<BookListItemProjection> buscarParaListaAdmin(Pageable pageable) {
        return bookRepository.findLivrosParaListaAdmin(pageable);
    }

    public Page<BookGroupedResponse> buscarLivrosAgrupados(Pageable pageable, String texto) {
        return bookRepository.findLivrosAgrupados(pageable, texto);
    }

    @Cacheable(value = BOOK_DETAIL, key = "#id")
    public Optional<Book> findById(UUID id) {
        log.info("Buscando livro ID {} no banco de dados (sem cache)...", id);
        return bookRepository.findByIdWithDetails(id);
    }

    public long countAvailableCopies(UUID bookId) {
        return bookCopyRepository.countByBookIdAndStatus(bookId,
                br.com.lumilivre.api.enums.BookCopyStatus.AVAILABLE);
    }

    public long countTotalCopies(UUID bookId) {
        return bookCopyRepository.countByBook_Id(bookId);
    }

    @Cacheable(MOBILE_CATALOG)
    public List<BookCatalogResponse> buscarCatalogoParaMobile() {
        log.info("Buscando catálogo mobile no banco de dados (sem cache)...");
        List<Map<String, Object>> results = bookRepository.findCatalogoMobile();

        Map<String, List<BookCardResponse>> livrosPorGenero = results.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get("genrename"),
                        Collectors.mapping(row -> BookCardResponse.builder()
                                .id(UUID.fromString(row.get("id").toString()))
                                .title((String) row.get("title"))
                                .author((String) row.get("author"))
                                .coverUrl((String) row.get("coverurl"))
                                .rating(row.get("rating") != null ? ((Number) row.get("rating")).doubleValue() : 4.6)
                                .build(),
                                Collectors.toList())));

        return livrosPorGenero.entrySet().stream()
                .map(entry -> BookCatalogResponse.builder()
                        .genreName(entry.getKey())
                        .books(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(BookCatalogResponse::getGenreName))
                .collect(Collectors.toList());
    }

    public Page<BookCardResponse> buscarPorGenero(String nomeGenero, Pageable pageable) {
        return bookRepository.findByGeneroAsCatalogoDTO(nomeGenero, pageable);
    }

    public Page<BookListItemProjection> buscarPorTexto(String texto, Pageable pageable) {
        return bookRepository.findLivrosParaListaAdmin(pageable);
    }

    @Cacheable(value = BOOK_COUNT)
    public long getContagemLivros() {
        return bookRepository.count();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
            @CacheEvict(value = BOOK_DETAIL, key = "#id")
    })
    public Book uploadCapa(UUID id, MultipartFile file) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found-for-id", id));

        if (file != null && !file.isEmpty()) {
            try {
                String url = storageService.uploadFile(file, "covers");
                book.setCoverUrl(url);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        }

        return bookRepository.save(book);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
            @CacheEvict(value = BOOK_COUNT, allEntries = true)
    })
    public Book cadastrar(BookRequest request, MultipartFile file) {
        if (isNaoVazio(request.getIsbn()) && bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            throw BusinessRuleException.ofKey("book.isbn.already-exists");
        }

        if (isNaoVazio(request.getIsbn())) {
            preencherDadosExternos(request);
        }

        validarCampos(request);

        try {
            Book book = montarLivro(new Book(), request, file);
            return bookRepository.save(book);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao montar ou salvar o livro: {}", e.getMessage(), e);
            throw BusinessRuleException.ofKey("book.create.failed");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
    })
    public Book atualizar(UUID id, BookRequest request, MultipartFile file) {
        Book bookToUpdate = bookRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found-for-id", id));

        if (isNaoVazio(request.getIsbn())) {
            Optional<Book> livroComMesmoIsbn = bookRepository.findByIsbn(request.getIsbn());
            if (livroComMesmoIsbn.isPresent() && !livroComMesmoIsbn.get().getId().equals(id)) {
                throw BusinessRuleException.ofKey("book.isbn.belongs-to-other");
            }
        } else {
            request.setIsbn(bookToUpdate.getIsbn());
        }

        if (isNaoVazio(request.getIsbn())) {
            preencherDadosExternos(request);
        }

        validarCampos(request);

        try {
            Book updatedBook = montarLivro(bookToUpdate, request, file);
            return bookRepository.save(updatedBook);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao montar ou atualizar o livro ID {}: {}", id, e.getMessage(), e);
            throw BusinessRuleException.ofKey("book.update.failed");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = MOBILE_CATALOG, allEntries = true),
    })
    public void excluirLivroComExemplares(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw ResourceNotFoundException.ofKey("book.not-found");
        }
        bookCopyRepository.deleteAllByBook_Id(id);
        bookRepository.deleteById(id);
    }

    private void preencherDadosExternos(BookRequest request) {
        boolean googleEncontrou = false;
        boolean temCapa = isNaoVazio(request.getCoverUrl());

        try {
            var googleOpt = googleBooksService.findBestBookMatch(request.getIsbn(), request.getTitle(), request.getAuthor());

            if (googleOpt.isPresent()) {
                googleEncontrou = true;
                var livroGoogle = googleOpt.get().livro();
                var googleData = googleOpt.get();

                if (isVazio(request.getTitle())) request.setTitle(livroGoogle.getTitle());
                if (isVazio(request.getPublisher())) request.setPublisher(livroGoogle.getPublisher());
                if (request.getPageCount() == null || request.getPageCount() == 0)
                    request.setPageCount(livroGoogle.getPageCount());
                if (request.getPublicationDate() == null) request.setPublicationDate(livroGoogle.getPublicationDate());
                if (isVazio(request.getSynopsis())) request.setSynopsis(livroGoogle.getSynopsis());
                if (isVazio(request.getAuthor()) && isNaoVazio(livroGoogle.getAuthor()))
                    request.setAuthor(livroGoogle.getAuthor());

                if (!temCapa && isNaoVazio(livroGoogle.getCoverUrl())) {
                    request.setCoverUrl(livroGoogle.getCoverUrl());
                    temCapa = true;
                }

                if (googleData.averageRating() != null) {
                    request.setRating(googleData.averageRating());
                } else if (request.getRating() == null) {
                    request.setRating(4.6);
                }
            }
        } catch (Exception e) {
            log.warn("Falha na busca Google Books: {}", e.getMessage());
        }

        if ((!googleEncontrou || !temCapa) && isNaoVazio(request.getIsbn())) {
            try {
                var brasilOpt = brasilApiService.buscarPorIsbn(request.getIsbn());

                if (brasilOpt.isPresent()) {
                    var brData = brasilOpt.get();
                    if (isVazio(request.getTitle())) request.setTitle(brData.title());
                    if (isVazio(request.getPublisher())) request.setPublisher(brData.publisher());
                    if (isVazio(request.getSynopsis())) request.setSynopsis(brData.synopsis());
                    if (request.getPageCount() == null || request.getPageCount() == 0)
                        request.setPageCount(brData.pageCount());
                    if (request.getPublicationDate() == null && brData.year() != null)
                        request.setPublicationDate(LocalDate.of(brData.year(), 1, 1));
                    if (isVazio(request.getAuthor()) && brData.authors() != null && !brData.authors().isEmpty())
                        request.setAuthor(String.join(", ", brData.authors()));
                    if (!temCapa && isNaoVazio(brData.coverUrl()))
                        request.setCoverUrl(brData.coverUrl());
                }
            } catch (Exception e) {
                log.warn("Falha no fallback BrasilAPI: {}", e.getMessage());
            }
        }

        if (request.getRating() == null) request.setRating(4.6);
    }

    private void validarCampos(BookRequest request) {
        if (isVazio(request.getTitle())) throw BusinessRuleException.ofKey("book.form.title.required");
        if (request.getPublicationDate() != null && request.getPublicationDate().isAfter(LocalDate.now()))
            throw BusinessRuleException.ofKey("book.form.publication-date.future-not-allowed");
        if (isVazio(request.getPublisher())) throw BusinessRuleException.ofKey("book.form.publisher.required");
        if (isVazio(request.getAuthor())) throw BusinessRuleException.ofKey("book.form.author.required");
    }

    private Book montarLivro(Book book, BookRequest request, MultipartFile file) {
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setPublicationDate(request.getPublicationDate());
        book.setPageCount(request.getPageCount());
        book.setPublisher(request.getPublisher());
        book.setEdition(request.getEdition());
        book.setVolume(request.getVolume());
        book.setSynopsis(request.getSynopsis());
        book.setAuthor(request.getAuthor());
        book.setRating(request.getRating() != null ? request.getRating() : 4.6);

        if (isNaoVazio(request.getDeweyCode())) {
            DeweyClassification cdd = deweyClassificationRepository.findById(request.getDeweyCode())
                    .orElseThrow(() -> BusinessRuleException.ofKey("book.dewey-code.invalid", request.getDeweyCode()));
            book.setDeweyClassification(cdd);
        } else {
            book.setDeweyClassification(null);
        }

        Set<Genre> genres = new HashSet<>();
        if (request.getGenres() != null && !request.getGenres().isEmpty()) {
            genres = genreRepository.findByNameIn(request.getGenres()).stream()
                    .limit(3)
                    .collect(Collectors.toSet());
        }
        book.setGenres(genres);

        try {
            if (isNaoVazio(request.getAgeRating())) {
                book.setAgeRating(AgeRating.valueOf(request.getAgeRating().toUpperCase()));
            }
            book.setCoverType(isNaoVazio(request.getCoverType())
                    ? CoverType.valueOf(request.getCoverType().toUpperCase())
                    : null);
        } catch (IllegalArgumentException e) {
            throw BusinessRuleException.ofKey("book.enum.age-rating-or-cover-type.invalid");
        }

        if (file != null && !file.isEmpty()) {
            try {
                book.setCoverUrl(storageService.uploadFile(file, "covers"));
            } catch (Exception e) {
                log.error("Erro ao enviar a capa: {}", e.getMessage(), e);
                throw BusinessRuleException.ofKey("book.cover.upload-failed");
            }
        } else if (isNaoVazio(request.getCoverUrl())) {
            book.setCoverUrl(request.getCoverUrl());
        }

        return book;
    }

    public BookRequest pesquisarDadosPorIsbn(String isbn) {
        BookRequest request = new BookRequest();
        request.setIsbn(isbn);
        preencherDadosExternos(request);
        if (isVazio(request.getTitle())) {
            throw ResourceNotFoundException.ofKey("book.external.not-found-for-isbn", isbn);
        }
        return request;
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
