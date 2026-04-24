package br.com.lumilivre.api.domain.policy;

import java.time.LocalDateTime;
import java.util.List;

import br.com.lumilivre.api.enums.StatusReserva;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;

/** Pure domain policy — sem Spring. */
public final class ReservationPolicy {

    /** Dias de tolerância para o aluno retirar após ser notificado */
    public static final int PICKUP_DEADLINE_DAYS = 2;

    /** Máximo de reservas ativas simultâneas por aluno */
    public static final int MAX_ACTIVE_RESERVATIONS = 3;

    private static final List<StatusReserva> ACTIVE_STATUSES =
            List.of(StatusReserva.WAITING, StatusReserva.READY);

    private ReservationPolicy() {}

    /**
     * Valida se o aluno pode criar uma nova reserva.
     *
     * @param penaltyExpiry data de expiração de penalidade ativa (null = sem penalidade)
     * @param activeReservations reservas ativas atuais do aluno
     * @param alreadyReserved   se já existe reserva ativa deste aluno para o mesmo livro
     */
    public static void validateNewReservation(
            LocalDateTime penaltyExpiry,
            long activeReservations,
            boolean alreadyReserved) {

        if (penaltyExpiry != null && penaltyExpiry.isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException(
                    "Aluno possui penalidade ativa até " + penaltyExpiry.toLocalDate() +
                    ". Reservas bloqueadas.");
        }
        if (alreadyReserved) {
            throw new RegraDeNegocioException("Aluno já possui uma reserva ativa para este livro.");
        }
        if (activeReservations >= MAX_ACTIVE_RESERVATIONS) {
            throw new RegraDeNegocioException(
                    "Limite de " + MAX_ACTIVE_RESERVATIONS + " reservas simultâneas atingido.");
        }
    }

    /**
     * Determina o prazo de retirada a partir do momento da notificação.
     */
    public static LocalDateTime calculatePickupDeadline(LocalDateTime notifiedAt) {
        return notifiedAt.plusDays(PICKUP_DEADLINE_DAYS);
    }

    /** Estados que constituem uma "reserva ativa" para fins de limite. */
    public static List<StatusReserva> activeStatuses() {
        return ACTIVE_STATUSES;
    }
}
