package br.com.lumilivre.api.controller;

import java.util.Locale;

import br.com.lumilivre.api.service.ImportService;
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
public class ImportController {

    private final ImportService importService;

    @PostMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importStudents(@RequestParam MultipartFile file, Locale locale) throws Exception {
        return ResponseEntity.ok(importService.importar("aluno", file, locale));
    }

    @PostMapping("/books")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importBooks(@RequestParam MultipartFile file, Locale locale) throws Exception {
        return ResponseEntity.ok(importService.importar("livro", file, locale));
    }

    @PostMapping("/copies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importCopies(@RequestParam MultipartFile file, Locale locale) throws Exception {
        return ResponseEntity.ok(importService.importar("exemplar", file, locale));
    }
}
