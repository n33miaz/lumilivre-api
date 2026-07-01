package br.com.lumilivre.api.controller;

import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.IMPORTS)
public class ImportController {

    private final ImportService importService;

    @PostMapping("/readers")
    @Operation(operationId = "imports.readers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importReaders(@RequestParam MultipartFile file, Locale locale) throws Exception {
        return ResponseEntity.ok(importService.importar("leitor", file, locale));
    }

    @PostMapping("/books")
    @Operation(operationId = "imports.books")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importBooks(@RequestParam MultipartFile file, Locale locale) throws Exception {
        return ResponseEntity.ok(importService.importar("livro", file, locale));
    }

    @PostMapping("/copies")
    @Operation(operationId = "imports.copies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importCopies(@RequestParam MultipartFile file, Locale locale) throws Exception {
        return ResponseEntity.ok(importService.importar("exemplar", file, locale));
    }
}
