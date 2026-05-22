package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.book.BookCopyRequest;
import br.com.lumilivre.api.dto.book.BookCopyResponse;
import br.com.lumilivre.api.mapper.BookCopyMapper;
import br.com.lumilivre.api.service.BookCopyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book-copies")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.BOOK_COPIES)
public class BookCopyController {

    private final BookCopyService bookCopyService;
    private final BookCopyMapper mapper;

    @GetMapping("/by-book/{bookId}")
    @Operation(operationId = "book-copies.byBook")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<BookCopyResponse>> listByBook(@PathVariable UUID bookId, Locale locale) {
        List<BookCopyResponse> copies = bookCopyService.buscarExemplaresPorLivroId(bookId)
                .stream()
                .map(copy -> mapper.toResponse(copy, locale))
                .toList();
        if (copies.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(copies);
    }

    @PostMapping
    @Operation(operationId = "book-copies.create")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> create(@Valid @RequestBody BookCopyRequest request) {
        bookCopyService.cadastrar(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{copyCode}")
    @Operation(operationId = "book-copies.update")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> update(@PathVariable String copyCode,
                                       @Valid @RequestBody BookCopyRequest request) {
        bookCopyService.atualizar(copyCode, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{copyCode}")
    @Operation(operationId = "book-copies.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable String copyCode) {
        bookCopyService.excluir(copyCode);
        return ResponseEntity.noContent().build();
    }
}
