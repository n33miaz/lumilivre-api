package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.book.BookRequest;
import br.com.lumilivre.api.dto.book.BookResponse;
import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.dto.book.BookCatalogResponse;
import br.com.lumilivre.api.dto.book.BookGroupedResponse;
import br.com.lumilivre.api.dto.book.BookSummaryResponse;
import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.mapper.BookMapper;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.security.AccessAudited;
import br.com.lumilivre.api.security.CanAccessReader;
import br.com.lumilivre.api.service.BookService;
import br.com.lumilivre.api.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.BOOKS)
public class BookController {

    private final BookService bookService;
    private final BookMapper mapper;
    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(operationId = "books.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<BookSummaryResponse>> list(
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<BookSummaryResponse> page = bookService
                .buscarParaListaAdmin(pageable)
                .map(p -> mapper.toSummary(p, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/search")
    @Operation(operationId = "books.search")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<BookSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<BookSummaryResponse> page = bookService
                .buscarPorTexto(q != null ? q : "", pageable)
                .map(p -> mapper.toSummary(p, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/grouped")
    @Operation(operationId = "books.grouped")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<BookGroupedResponse>> grouped(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<BookGroupedResponse> page = bookService.buscarLivrosAgrupados(pageable, q);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/advanced")
    @Operation(operationId = "books.advanced")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<BookGroupedResponse>> advanced(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String deweyCode,
            @RequestParam(required = false) String ageRating,
            @RequestParam(required = false) String coverType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publicationDate,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<BookGroupedResponse> page = bookService.buscarAvancado(
                title, isbn, author, genre, publisher, deweyCode,
                ageRating, coverType, publicationDate, pageable);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/public/search")
    @Operation(operationId = "books.publicSearch")
    @AccessAudited(event = AccessEvent.CATALOG_SEARCH)
    public ResponseEntity<Page<BookCardResponse>> publicSearch(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<BookCardResponse> page = bookService.buscarMobilePorTexto(q, pageable);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/catalog")
    @Operation(operationId = "books.catalog")
    @AccessAudited(event = AccessEvent.CATALOG_SEARCH)
    public ResponseEntity<List<BookCatalogResponse>> catalog(Locale locale) {
        List<BookCatalogResponse> body = bookService.buscarCatalogoParaMobile();
        if (body.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/genres/{genreName}")
    @Operation(operationId = "books.byGenre")
    @AccessAudited(event = AccessEvent.CATALOG_SEARCH)
    public ResponseEntity<Page<BookCardResponse>> byGenre(
            @PathVariable String genreName,
            @PageableDefault(size = 10) Pageable pageable,
            Locale locale) {
        Page<BookCardResponse> page = bookService.buscarPorGenero(genreName, pageable);
        return page.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok()
                        .header("Content-Language", locale.toLanguageTag())
                        .body(page);
    }

    /**
     * As recomendacoes derivam do historico de emprestimos do leitor, e ficam em
     * cache com a matricula como chave. Sem o {@code @CanAccessReader} qualquer
     * LEITOR autenticado podia passar a matricula de outro e receber a lista dele
     * — inclusive servida do cache. ADMIN/BIBLIOTECARIO seguem podendo consultar
     * qualquer matricula.
     */
    @GetMapping("/recommendations/{registrationNumber}")
    @Operation(operationId = "books.recommendations")
    @CanAccessReader
    public ResponseEntity<List<BookCardResponse>> recommendations(
            @PathVariable String registrationNumber,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(recommendationService.recommendForReader(registrationNumber));
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(operationId = "books.isbnLookup")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<BookRequest> isbnLookup(@PathVariable String isbn, Locale locale) {
        BookRequest body = bookService.pesquisarDadosPorIsbn(isbn);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    /**
     * Ficha do livro, aberta ao convidado.
     *
     * <p>Era o único ponto do catálogo que exigia papel, enquanto
     * {@code /catalog}, {@code /public/search} e {@code /genres/**} já eram
     * públicos: o convidado listava os livros e tomava 401 ao tocar em um deles,
     * o que o app mostrava como erro de rede.
     *
     * <p>Abrir foi decisão de campo por campo, não de conveniência.
     * {@link BookResponse} devolve apenas dado bibliográfico — ISBN, título,
     * autor, editora, data, páginas, sinopse, capa, CDD, faixa etária, tipo de
     * capa, edição, volume, nota e gêneros. Nada de exemplar (tombo,
     * localização física, status), nada de empréstimo, nada de pessoa. É o
     * conteúdo da lombada e da ficha catalográfica, que qualquer OPAC de
     * biblioteca pública expõe, e metade dele já saía por {@code /catalog}.
     * Enquanto isso valer, não há projeção pública a criar: ela seria cópia do
     * DTO inteiro.
     *
     * <p>A esse recorte somam-se agora as <b>contagens</b> de exemplares (total
     * e disponíveis). Contagem não identifica exemplar: não diz tombo nem
     * prateleira, e é o mesmo "disponível / emprestado" que qualquer estante
     * virtual de biblioteca pública mostra. Sem ela o app não tinha como saber
     * se dava para pedir emprestado — lia a ausência do campo como zero e o
     * botão de solicitar empréstimo ficava morto em todo livro para todo leitor.
     */
    @GetMapping("/{id}")
    @Operation(operationId = "books.get")
    @PreAuthorize("permitAll()")
    @AccessAudited(event = AccessEvent.BOOK_VIEWED, targetParam = "#id")
    public ResponseEntity<BookResponse> getOne(@PathVariable UUID id, Locale locale) {
        BookResponse body = bookService.findById(id)
                .map(book -> mapper.toResponse(book, locale, bookService.contarExemplares(id)))
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found"));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping
    @Operation(operationId = "books.create")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<BookResponse> create(
            @Valid @RequestBody BookRequest request,
            Locale locale) {
        // Livro recém-criado ainda não tem exemplar, mas a contagem sai como 0/0
        // explícito: campo ausente foi lido como zero por um cliente e é
        // justamente a ambiguidade que este DTO deixou de ter.
        Book created = bookService.cadastrar(request, null);
        BookResponse body = mapper.toResponse(created, locale, bookService.contarExemplares(created.getId()));
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "books.update")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<BookResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody BookRequest request,
            Locale locale) {
        BookResponse body = mapper.toResponse(
                bookService.atualizar(id, request, null), locale, bookService.contarExemplares(id));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "books.uploadCover")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<BookResponse> uploadCover(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Locale locale) {
        BookResponse body = mapper.toResponse(
                bookService.uploadCapa(id, file), locale, bookService.contarExemplares(id));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "books.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookService.excluirLivroComExemplares(id);
        return ResponseEntity.noContent().build();
    }
}
