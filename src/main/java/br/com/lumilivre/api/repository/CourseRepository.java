package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.v1.curso.CursoEstatisticaResponse;
import br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse;
import br.com.lumilivre.api.model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    Optional<Course> findByNameIgnoreCase(String name);

    @Query("""
            SELECT c FROM Course c
            WHERE c.name ILIKE :texto
            """)
    Page<Course> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT c FROM Course c
            WHERE (:nome IS NULL OR c.name ILIKE :nome)
            """)
    Page<Course> buscarAvancado(@Param("nome") String nome, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse(c.name)
            FROM Course c
            ORDER BY c.name
            """)
    Page<CursoResumoResponse> findCursoParaListaAdmin(Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.curso.CursoEstatisticaResponse(
                c.name,
                COUNT(DISTINCT a.id),
                COUNT(l)
            )
            FROM Course c
            LEFT JOIN c.students a
            LEFT JOIN Loan l ON l.student = a
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<CursoEstatisticaResponse> findEstatisticasCursos();

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse(c.id, c.name, COUNT(a))
            FROM Course c
            LEFT JOIN c.students a
            WHERE (:texto IS NULL OR c.name ILIKE :texto)
            GROUP BY c.id, c.name
            """)
    Page<CursoResumoResponse> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse(c.id, c.name, COUNT(a))
            FROM Course c
            LEFT JOIN c.students a
            WHERE (:nome IS NULL OR c.name ILIKE :nome)
            GROUP BY c.id, c.name
            """)
    Page<CursoResumoResponse> buscarAvancadoComDTO(@Param("nome") String nome, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse(c.id, c.name, COUNT(a))
            FROM Course c
            LEFT JOIN c.students a
            WHERE (:texto IS NULL OR c.name ILIKE %:texto%)
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    Page<CursoResumoResponse> findCursoParaListaAdminComFiltro(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse(c.name, COUNT(l))
            FROM Course c
            JOIN c.students a
            JOIN Loan l ON l.student = a
            GROUP BY c.name
            HAVING COUNT(l) > 0
            """)
    List<br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse> findTotalEmprestimosPorCurso();
}
