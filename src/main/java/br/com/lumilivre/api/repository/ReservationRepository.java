package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.model.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findFirstByBook_IdAndStatusOrderByQueuePositionAsc(UUID bookId, ReservationStatus status);

    List<Reservation> findByReader_RegistrationNumberOrderByCreatedAtDesc(String registrationNumber);

    boolean existsByReader_RegistrationNumberAndBook_IdAndStatusIn(
            String registrationNumber, UUID bookId, List<ReservationStatus> statuses);

    @Query("SELECT COALESCE(MAX(r.queuePosition), 0) FROM Reservation r WHERE r.book.id = :bookId AND r.status = 'WAITING'")
    int maxQueuePosition(@Param("bookId") UUID bookId);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, OffsetDateTime now);
}
