package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ItemSimplesDTO;
import br.com.lumilivre.api.repository.TurnoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/turnos")
@Tag(name = "15. Turnos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class TurnoController {

    private final TurnoRepository turnoRepository;

    public TurnoController(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    @GetMapping
    @Operation(summary = "Lista todos os turnos cadastrados")
    public ResponseEntity<List<ItemSimplesDTO>> listarTodos() {
        var lista = turnoRepository.findAll().stream()
                .map(t -> new ItemSimplesDTO(t.getId(), t.getNome()))
                .toList();

        if (lista.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lista);
    }
}