package br.com.lumilivre.api.controller.v1;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.lumilivre.api.dto.v1.comum.ItemSimplesResponse;
import br.com.lumilivre.api.repository.GenreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/generos")
@Tag(name = "12. Gêneros")
@SecurityRequirement(name = "bearerAuth")
public class GeneroController {

    private final GenreRepository genreRepository;

    public GeneroController(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Lista todos os gêneros cadastrados")
    public ResponseEntity<List<ItemSimplesResponse>> listarTodos() {
        var lista = genreRepository.findAll().stream()
                .map(g -> new ItemSimplesResponse(g.getId(), g.getName()))
                .toList();
        return ResponseEntity.ok(lista);
    }
}
