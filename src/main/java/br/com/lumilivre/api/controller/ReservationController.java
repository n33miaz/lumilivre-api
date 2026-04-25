package br.com.lumilivre.api.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Fila de reserva de livros (FIFO)")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @CanAccessStudent
    @PostMapping
    @Operation(summary = "Cria uma reserva para o próximo exemplar disponível de um livro")
    public ResponseEntity<ApiResponse<Reservation>> criar(
            @RequestParam String matricula,
            @RequestParam UUID livroId) {
        Reservation reserva = reservationService.criarReserva(matricula, livroId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reserva criada com sucesso.", reserva));
    }

    @CanAccessStudent
    @DeleteMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma reserva do próprio aluno")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable UUID id,
            @RequestParam String matricula) {
        reservationService.cancelarReserva(id, matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reserva cancelada.", null));
    }
}
