package br.com.lumilivre.api.controller.v2;

import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.dto.reservation.ReservationRequest;
import br.com.lumilivre.api.dto.reservation.ReservationResponse;
import br.com.lumilivre.api.mapper.v2.ReservationMapper;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.service.ReservationService;
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
@RequestMapping("/api/v2/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper mapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            Locale locale) {
        Reservation saved = reservationService.criarReserva(
                request.getStudentRegistrationNumber(), request.getBookId());
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(saved, locale));
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestParam String studentRegistrationNumber) {
        reservationService.cancelarReserva(id, studentRegistrationNumber);
        return ResponseEntity.noContent().build();
    }
}
