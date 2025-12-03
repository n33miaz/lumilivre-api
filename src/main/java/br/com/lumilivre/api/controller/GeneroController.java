package br.com.lumilivre.api.controller;

import java.util.List;
import br.com.lumilivre.api.dto.comum.ItemSimplesResponse;
import br.com.lumilivre.api.repository.GeneroRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/generos")
@Tag(name = "10. Gêneros")
@SecurityRequirement(name = "bearerAuth")
public class GeneroController {

    @Autowired
    private GeneroRepository generoRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    @Operation(summary = "Lista todos os gêneros cadastrados")
    public ResponseEntity<List<ItemSimplesResponse>> listarTodos() {
        var lista = generoRepository.findAll().stream()
                .map(g -> new ItemSimplesResponse(g.getId(), g.getNome()))
                .toList();
        return ResponseEntity.ok(lista);
    }
}