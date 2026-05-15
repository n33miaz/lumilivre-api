package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.dto.v1.turno.TurnoResumoResponse;
import br.com.lumilivre.api.model.StudyShift;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import java.util.List;

public interface StudyShiftRepository extends JpaRepository<StudyShift, Integer> {

    @Cacheable("turnos")
    @Override
    @NonNull
    List<StudyShift> findAll();

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.turno.TurnoResumoResponse(t.id, t.name, COUNT(a))
            FROM StudyShift t
            LEFT JOIN t.students a
            WHERE (:texto IS NULL OR t.name ILIKE %:texto%)
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    Page<TurnoResumoResponse> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse(t.name, COUNT(l))
            FROM StudyShift t
            JOIN t.students a
            JOIN Loan l ON l.student = a
            GROUP BY t.name
            HAVING COUNT(l) > 0
            """)
    List<br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse> findTotalEmprestimosPorTurno();
}
