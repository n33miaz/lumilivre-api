package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.genre.GenreResponse;
import br.com.lumilivre.api.repository.GenreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.GENRES)
public class GenreController {

    private final GenreRepository genreRepository;

    @GetMapping
    @Operation(operationId = "genres.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<GenreResponse>> list(Locale locale) {
        List<GenreResponse> body = genreRepository.findAll()
                .stream()
                .map(g -> new GenreResponse(g.getId(), g.getName()))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }
}
