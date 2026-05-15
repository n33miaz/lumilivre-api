package br.com.lumilivre.api.controller.v2;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.dto.genre.GenreResponse;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreRepository genreRepository;

    @GetMapping
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
