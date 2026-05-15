package br.com.lumilivre.api.controller.v2;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.dto.book.BookCopyRequest;
import br.com.lumilivre.api.dto.book.BookCopyResponse;
import br.com.lumilivre.api.mapper.v2.BookCopyMapper;
import br.com.lumilivre.api.service.BookCopyService;
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
@RequestMapping("/api/v2/book-copies")
@RequiredArgsConstructor
public class BookCopyController {

    private final BookCopyService bookCopyService;
    private final BookCopyMapper mapper;

    @GetMapping("/by-book/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<BookCopyResponse>> listByBook(@PathVariable UUID bookId, Locale locale) {
        List<BookCopyResponse> copies = bookCopyService.buscarExemplaresPorLivroId(bookId)
                .stream()
                .map(v1 -> mapper.fromV1(v1, bookId, locale))
                .toList();
        if (copies.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(copies);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> create(@Valid @RequestBody BookCopyRequest request) {
        bookCopyService.cadastrar(mapper.toV1Request(request));
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{copyCode}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> update(@PathVariable String copyCode,
                                       @Valid @RequestBody BookCopyRequest request) {
        bookCopyService.atualizar(copyCode, mapper.toV1Request(request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{copyCode}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable String copyCode) {
        bookCopyService.excluir(copyCode);
        return ResponseEntity.noContent().build();
    }
}
