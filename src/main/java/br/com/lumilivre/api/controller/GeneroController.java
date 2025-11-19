package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ItemSimplesDTO;
import br.com.lumilivre.api.repository.GeneroRepository;
import br.com.lumilivre.api.service.GeneroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/generos")
@Tag(name = "10. Gêneros")
@SecurityRequirement(name = "bearerAuth")
public class GeneroController {

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private GeneroService generoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Lista todos os gêneros cadastrados")
    public ResponseEntity<List<ItemSimplesDTO>> listarTodos() {
        var lista = generoRepository.findAll().stream()
                .map(g -> new ItemSimplesDTO(g.getId(), g.getNome()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/sugestao-por-cdd/{cddCodigo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Sugere gêneros com base em um código CDD")
    public ResponseEntity<List<ItemSimplesDTO>> sugerirPorCdd(@PathVariable String cddCodigo) {
        var generosSugeridos = generoService.sugerirGenerosPorCdd(cddCodigo).stream()
                .map(g -> new ItemSimplesDTO(g.getId(), g.getNome()))
                .collect(Collectors.toList());

        if (generosSugeridos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(generosSugeridos);
    }
}