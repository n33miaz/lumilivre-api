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
import br.com.lumilivre.api.security.Auditable;
import br.com.lumilivre.api.service.infra.bookmetadata.BookMetadata;
import br.com.lumilivre.api.service.infra.bookmetadata.BookMetadataChain;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import br.com.lumilivre.api.utils.SortAllowlist;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    /**
     * Colunas ordenaveis da listagem administrativa de exemplares.
     *
     * <p>{@code findLivrosParaListaAdmin} e a unica query nativa que recebe
     * {@link Pageable} do request, e numa query nativa o {@code ORDER BY} do
     * sort e interpolado como texto. Sem este mapa, o nome da coluna vem do
     * cliente; com ele, o que chega na consulta e sempre uma destas constantes.
     *
     * <p>Sobre a forma das colunas: o Spring Data prefixa a propriedade com o
     * alias primario da query nativa ({@code e}, de {@code book_copy}) a menos
     * que ela ja comece por um alias de join. Por isso as colunas de
     * {@code book_copy} vao sem qualificador (viram {@code e.status}) e as de
     * {@code book} vao com {@code l.} (ficam intactas). Os nomes nao colidem
     * entre as duas tabelas, entao vale nos dois casos — e o
     * OptionalFilterQueriesPostgresTest ordena por cada um deles contra Postgres
     * de verdade, que foi o que pegou a versao anterior deste mapa.
     */
    private static final SortAllowlist LISTA_ADMIN_SORT = SortAllowlist.of(
            "status", "status",
            "copyCode", "copy_code",
            "physicalLocation", "shelf_location",
            "isbn", "l.isbn",
            "deweyCode", "l.dewey_code",
            "title", "l.title",
            "author", "l.author",
            "publisher", "l.publisher");

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final StorageProvider storageProvider;
    private final BookMetadataChain bookMetadataChain;
    private final GenreRepository genreRepository;
    private final DeweyClassificationRepository deweyClassificationRepository;

    public BookService(BookCopyRepository bookCopyRepository, BookRepository bookRepository,
            StorageProvider storageProvider, BookMetadataChain bookMetadataChain,
            GenreRepository genreRepository, DeweyClassificationRepository deweyClassificationRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.storageProvider = storageProvider;
        this.bookMetadataChain = bookMetadataChain;
        this.genreRepository = genreRepository;
        this.deweyClassificationRepository = deweyClassificationRepository;
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
        return bookRepository.findLivrosParaListaAdmin(LISTA_ADMIN_SORT.sanitize(pageable));
    }

    public Page<BookGroupedResponse> buscarLivrosAgrupados(Pageable pageable, String texto) {
        return bookRepository.findLivrosAgrupados(pageable, texto);
    }

    /**
     * {@code unless} porque a ficha do livro passou a ser pública: o cache é um
     * {@code ConcurrentMapCache} sem limite de tamanho, então guardar o
     * {@code Optional.empty()} de cada id inexistente deixava qualquer anônimo
     * encher a heap disparando UUID aleatório — amplificação bem mais barata
     * para o atacante que o custo da própria consulta. Miss só toca o banco, que
     * responde por índice de chave primária.
     *
     * <p>{@code #result == null} e não {@code #result.isEmpty()}: o Spring
     * desembrulha o {@link Optional} antes de avaliar a expressão, então
     * {@code #result} aqui é o {@code Book} (ou {@code null} para
     * {@code Optional.empty()}). Chamar {@code isEmpty()} sobre ele estoura em
     * SpEL e transforma a ficha do livro num 500.
     */
    @Cacheable(value = BOOK_DETAIL, key = "#id", unless = "#result == null")
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
        return bookRepository.findLivrosParaListaAdmin(LISTA_ADMIN_SORT.sanitize(pageable));
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
                book.setCoverUrl(storageProvider.upload(file, StorageBucket.COVERS));
            } catch (Exception e) {
                log.error("Erro ao enviar a capa via {}: {}", storageProvider.name(), e.getMessage(), e);
                throw BusinessRuleException.ofKey("book.cover.upload-failed");
            }
        }

        return bookRepository.save(book);
    }

    // Alvo pelo retorno: o UUID so existe depois do save, e usar o ISBN
    // deixaria fora do audit livro cadastrado sem ISBN.
    @Auditable(action = "BOOK_CREATED", targetParam = "#result.id")
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

    @Auditable(action = "BOOK_UPDATED", targetParam = "#id")
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

    @Auditable(action = "BOOK_DELETED", targetParam = "#id")
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
        Optional<BookMetadata> result = Optional.empty();

        if (isNaoVazio(request.getIsbn())) {
            result = bookMetadataChain.findByIsbn(request.getIsbn());
        }
        if (result.isEmpty() && isNaoVazio(request.getTitle())) {
            result = bookMetadataChain.findByTitleAndAuthor(request.getTitle(), request.getAuthor());
        }

        result.ifPresent(metadata -> {
            log.info("Metadados externos resolvidos via providers='{}'", metadata.providerName());
            if (isVazio(request.getTitle())) request.setTitle(metadata.title());
            if (isVazio(request.getPublisher())) request.setPublisher(metadata.publisher());
            if (isVazio(request.getSynopsis())) request.setSynopsis(metadata.synopsis());
            if (isVazio(request.getAuthor()) && isNaoVazio(metadata.author()))
                request.setAuthor(metadata.author());
            if (request.getPageCount() == null || request.getPageCount() == 0)
                request.setPageCount(metadata.pageCount());
            if (request.getPublicationDate() == null && metadata.publicationDate() != null)
                request.setPublicationDate(metadata.publicationDate());
            if (isVazio(request.getCoverUrl()) && isNaoVazio(metadata.coverUrl()))
                request.setCoverUrl(metadata.coverUrl());
            if (request.getRating() == null && metadata.rating() != null)
                request.setRating(metadata.rating());
        });

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
                book.setCoverUrl(storageProvider.upload(file, StorageBucket.COVERS));
            } catch (Exception e) {
                log.error("Erro ao enviar a capa via {}: {}", storageProvider.name(), e.getMessage(), e);
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
