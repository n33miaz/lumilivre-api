package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.responses.ModuloResponseDTO;
import br.com.lumilivre.api.model.ModuloModel;
import br.com.lumilivre.api.repository.ModuloRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/modulos")
@Tag(name = "14. Módulos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class ModuloController {

    private final ModuloRepository moduloRepository;

    public ModuloController(ModuloRepository moduloRepository) {
        this.moduloRepository = moduloRepository;
    }

    @GetMapping
    @Operation(summary = "Lista todos os módulos cadastrados")
    public ResponseEntity<List<ModuloResponseDTO>> listarTodos() {
        List<ModuloModel> modulos = moduloRepository.findAll();

        if (modulos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<ModuloResponseDTO> dtos = modulos.stream()
                .map(ModuloResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}