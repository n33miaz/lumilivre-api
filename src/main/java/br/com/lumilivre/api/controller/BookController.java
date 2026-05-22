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
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.mapper.BookMapper;
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

    @GetMapping("/recommendations/{registrationNumber}")
    @Operation(operationId = "books.recommendations")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<BookCardResponse>> recommendations(
            @PathVariable String registrationNumber,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(recommendationService.recommendForStudent(registrationNumber));
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

    @GetMapping("/{id}")
    @Operation(operationId = "books.get")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<BookResponse> getOne(@PathVariable UUID id, Locale locale) {
        BookResponse body = bookService.findById(id)
                .map(book -> mapper.toResponse(book, locale))
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
        BookResponse body = mapper.toResponse(bookService.cadastrar(request, null), locale);
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
        BookResponse body = mapper.toResponse(bookService.atualizar(id, request, null), locale);
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
        BookResponse body = mapper.toResponse(bookService.uploadCapa(id, file), locale);
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
