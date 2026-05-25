package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import br.com.lumilivre.api.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @Mock
    private MessageResolver messages;

    @InjectMocks
    private ReservationService service;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messages.resolve(anyString(), any(Locale.class)))
                .thenAnswer(this::resolveMessage);
        lenient().when(messages.resolve(anyString(), any(Locale.class), any()))
                .thenAnswer(this::resolveMessage);
        lenient().when(messages.resolve(anyString(), any(Locale.class), any(), any()))
                .thenAnswer(this::resolveMessage);
    }

    private String resolveMessage(InvocationOnMock invocation) {
        String key = invocation.getArgument(0);
        Object[] args = invocation.getArguments();
        String first = args.length >= 3 && args[2] != null ? String.valueOf(args[2]) : "";
        String second = args.length >= 4 && args[3] != null ? String.valueOf(args[3]) : "";
        return switch (key) {
            case "email.reservation-registered.subject" -> "Reserva registrada";
            case "email.reservation-registered.body" ->
                "Sua reserva do livro '" + first + "' foi registrada (posição " + second + " na fila).";
            case "email.reservation-pickup.subject" -> "Livro disponível para retirada";
            case "email.reservation-pickup.body" ->
                "O livro '" + first + "' está disponível. Retire até " + second + ".";
            default -> key;
        };
    }

    @Test
    void criarReservaSavesNextQueuePositionAndPublishesOutbox() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student("12345")));
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book()));
        when(reservationRepository.findByStudent_RegistrationNumberOrderByCreatedAtDesc("12345")).thenReturn(List.of());
        when(reservationRepository.existsByStudent_RegistrationNumberAndBook_IdAndStatusIn(eq("12345"), eq(BOOK_ID), any()))
                .thenReturn(false);
        when(reservationRepository.maxQueuePosition(BOOK_ID)).thenReturn(2);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation reservation = service.criarReserva("12345", BOOK_ID);

        assertThat(reservation.getStudent().getRegistrationNumber()).isEqualTo("12345");
        assertThat(reservation.getBook().getId()).isEqualTo(BOOK_ID);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.WAITING);
        assertThat(reservation.getQueuePosition()).isEqualTo(3);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                eq("Reserva registrada"),
                org.mockito.ArgumentMatchers.contains("3"));
    }

    @Test
    void criarReservaRejectsDuplicateActiveReservation() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student("12345")));
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book()));
        when(reservationRepository.findByStudent_RegistrationNumberOrderByCreatedAtDesc("12345")).thenReturn(List.of());
        when(reservationRepository.existsByStudent_RegistrationNumberAndBook_IdAndStatusIn(eq("12345"), eq(BOOK_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criarReserva("12345", BOOK_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("reservation.policy.already-active");

        verify(reservationRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void cancelarReservaRequiresOwningStudent() {
        Reservation reservation = reservation("12345", ReservationStatus.WAITING);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancelarReserva(RESERVATION_ID, "99999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelarReservaMarksCancelledForOwner() {
        Reservation reservation = reservation("12345", ReservationStatus.WAITING);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        service.cancelarReserva(RESERVATION_ID, "12345");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void notificarProximoDaFilaMarksReadyAndPublishesOutbox() {
        Reservation reservation = reservation("12345", ReservationStatus.WAITING);
        when(reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(BOOK_ID, ReservationStatus.WAITING))
                .thenReturn(Optional.of(reservation));

        service.notificarProximoDaFila(BOOK_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(reservation.getNotifiedAt()).isNotNull();
        assertThat(reservation.getExpiresAt()).isAfter(reservation.getNotifiedAt());
        verify(reservationRepository).save(reservation);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("dispon"),
                org.mockito.ArgumentMatchers.contains("Livro Teste"));
    }

    @Test
    void expirarReservasVencidasMarksExpiredAndNotifiesNext() {
        Reservation expired = reservation("12345", ReservationStatus.READY);
        expired.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.READY), any()))
                .thenReturn(List.of(expired));
        when(reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(BOOK_ID, ReservationStatus.WAITING))
                .thenReturn(Optional.empty());

        service.expirarReservasVencidas();

        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(reservationRepository).save(expired);
    }

    private static Reservation reservation(String registrationNumber, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setStudent(student(registrationNumber));
        reservation.setBook(book());
        reservation.setStatus(status);
        reservation.setQueuePosition(1);
        return reservation;
    }

    private static Student student(String registrationNumber) {
        Student student = new Student();
        student.setRegistrationNumber(registrationNumber);
        student.setFullName("Aluno Teste");
        student.setEmail("aluno@lumilivre.test");
        return student;
    }

    private static Book book() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Livro Teste");
        return book;
    }
}
