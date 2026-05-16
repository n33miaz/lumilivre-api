package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.domain.policy.ReservationPolicy;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final OutboxPublisherService outboxPublisher;

    @Auditable(action = "RESERVATION_CREATED", targetParam = "#matricula")
    @Transactional
    public Reservation criarReserva(String matricula, UUID bookId) {
        Student student = studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado."));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado."));

        long activeReservations = reservationRepository
                .findByStudent_RegistrationNumberOrderByCreatedAtDesc(matricula)
                .stream()
                .filter(r -> ReservationPolicy.activeStatuses().contains(r.getStatus()))
                .count();

        boolean alreadyReserved = reservationRepository
                .existsByStudent_RegistrationNumberAndBook_IdAndStatusIn(
                        matricula, bookId, ReservationPolicy.activeStatuses());

        ReservationPolicy.validateNewReservation(
                student.getPenaltyExpiresAt(), activeReservations, alreadyReserved);

        int nextPosition = reservationRepository.maxQueuePosition(bookId) + 1;

        Reservation reservation = Reservation.builder()
                .student(student)
                .book(book)
                .queuePosition(nextPosition)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, student.getEmail(),
                "Reserva registrada",
                "Sua reserva do livro '" + book.getTitle() + "' foi registrada (posição " + nextPosition + " na fila).");

        return saved;
    }

    @Auditable(action = "RESERVATION_CANCELLED", targetParam = "#reservaId")
    @Transactional
    public void cancelarReserva(UUID reservaId, String matriculaCaller) {
        Reservation reservation = reservationRepository.findById(reservaId)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("reservation.not-found"));

        if (!reservation.getStudent().getRegistrationNumber().equals(matriculaCaller)) {
            throw ResourceNotFoundException.ofKey("reservation.not-found");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void notificarProximoDaFila(UUID bookId) {
        reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(
                bookId, ReservationStatus.WAITING)
                .ifPresent(next -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    next.setStatus(ReservationStatus.READY);
                    next.setNotifiedAt(now);
                    next.setExpiresAt(ReservationPolicy.calculatePickupDeadline(now));
                    reservationRepository.save(next);

                    outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                            next.getStudent().getEmail(),
                            "Livro disponível para retirada",
                            "O livro '" + next.getBook().getTitle() +
                            "' está disponível. Retire até " +
                            next.getExpiresAt().toLocalDate() + ".");
                });
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void expirarReservasVencidas() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.READY, OffsetDateTime.now());

        if (expired.isEmpty()) return;

        for (Reservation r : expired) {
            r.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(r);
            notificarProximoDaFila(r.getBook().getId());
        }

        log.info("ReservationService: {} reservation(s) expired.", expired.size());
    }
}
