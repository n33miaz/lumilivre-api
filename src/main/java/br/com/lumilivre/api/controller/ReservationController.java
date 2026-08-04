package br.com.lumilivre.api.controller;

import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.reservation.ReservationRequest;
import br.com.lumilivre.api.dto.reservation.ReservationResponse;
import br.com.lumilivre.api.mapper.ReservationMapper;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.RESERVATIONS)
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper mapper;

    @PostMapping
    @Operation(operationId = "reservations.create")
    // Leitor só cria reserva em nome próprio; ADMIN/BIBLIOTECARIO liberados (canAccess trata os papéis).
    @PreAuthorize("@readerAuthz.canAccess(#request.readerRegistrationNumber)")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            Locale locale) {
        Reservation saved = reservationService.criarReserva(
                request.getReaderRegistrationNumber(), request.getBookId());
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(saved, locale));
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(operationId = "reservations.cancel")
    // Só o dono da reserva (ou staff) pode cancelar.
    @PreAuthorize("@readerAuthz.canAccess(#readerRegistrationNumber)")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestParam String readerRegistrationNumber) {
        reservationService.cancelarReserva(id, readerRegistrationNumber);
        return ResponseEntity.noContent().build();
    }
}
