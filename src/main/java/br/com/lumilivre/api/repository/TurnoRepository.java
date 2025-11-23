package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.dto.turno.TurnoResumoResponse;
import br.com.lumilivre.api.model.TurnoModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import java.util.List;

public interface TurnoRepository extends JpaRepository<TurnoModel, Integer> {

    @Cacheable("turnos")
    @Override
    @NonNull
    List<TurnoModel> findAll();

    boolean existsByNomeIgnoreCase(String nome);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.turno.TurnoResumoResponse(t.id, t.nome, COUNT(a))
            FROM TurnoModel t
            LEFT JOIN t.alunos a
            WHERE (:texto IS NULL OR t.nome ILIKE %:texto%)
            GROUP BY t.id, t.nome
            ORDER BY t.nome
            """)
    Page<TurnoResumoResponse> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT new br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse(t.nome, SUM(a.emprestimosCount)) FROM TurnoModel t JOIN t.alunos a GROUP BY t.nome HAVING SUM(a.emprestimosCount) > 0")
    List<br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse> findTotalEmprestimosPorTurno();
}