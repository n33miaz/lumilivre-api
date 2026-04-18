package br.com.lumilivre.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.model.ReservaModel;
import br.com.lumilivre.api.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Fila de reserva de livros (FIFO)")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private final ReservaService reservaService;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PostMapping
    @Operation(summary = "Cria uma reserva para o próximo exemplar disponível de um livro")
    public ResponseEntity<ApiResponse<ReservaModel>> criar(
            @RequestParam String matricula,
            @RequestParam Long livroId) {
        ReservaModel reserva = reservaService.criarReserva(matricula, livroId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reserva criada com sucesso.", reserva));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @DeleteMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma reserva do próprio aluno")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable Long id,
            @RequestParam String matricula) {
        reservaService.cancelarReserva(id, matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reserva cancelada.", null));
    }
}
