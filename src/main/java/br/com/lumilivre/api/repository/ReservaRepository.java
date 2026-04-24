package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.enums.StatusReserva;
import br.com.lumilivre.api.model.ReservaModel;

@Repository
public interface ReservaRepository extends JpaRepository<ReservaModel, Long> {

    /** Próximo da fila FIFO para um livro específico */
    Optional<ReservaModel> findFirstByLivroIdAndStatusOrderByPosicaoFilaAsc(Long livroId, StatusReserva status);

    List<ReservaModel> findByAlunoRegistrationNumberOrderByCriadaEmDesc(String registrationNumber);

    boolean existsByAlunoRegistrationNumberAndLivroIdAndStatusIn(String registrationNumber, Long livroId, List<StatusReserva> statuses);

    @Query("SELECT COALESCE(MAX(r.posicaoFila), 0) FROM ReservaModel r WHERE r.livro.id = :livroId AND r.status = 'WAITING'")
    int maxPosicaoFila(@Param("livroId") Long livroId);

    /** Reservas DISPONIVEL_PARA_RETIRADA com prazo expirado */
    List<ReservaModel> findByStatusAndExpiraEmBefore(StatusReserva status, LocalDateTime now);

    default List<ReservaModel> findByAlunoMatriculaOrderByCriadaEmDesc(String matricula) {
        return findByAlunoRegistrationNumberOrderByCriadaEmDesc(matricula);
    }

    default boolean existsByAlunoMatriculaAndLivroIdAndStatusIn(String matricula, Long livroId, List<StatusReserva> statuses) {
        return existsByAlunoRegistrationNumberAndLivroIdAndStatusIn(matricula, livroId, statuses);
    }
}
