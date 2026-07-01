package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.dto.course.CourseSummaryResponse;
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
            SELECT new br.com.lumilivre.api.dto.course.CourseStatisticsResponse(
                c.name,
                COUNT(DISTINCT a.id),
                COUNT(l)
            )
            FROM Course c
            LEFT JOIN c.readers a
            LEFT JOIN Loan l ON l.reader = a
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<CourseStatisticsResponse> findStatistics();

    @Query("""
            SELECT new br.com.lumilivre.api.dto.course.CourseSummaryResponse(c.id, c.name, COUNT(a))
            FROM Course c
            LEFT JOIN c.readers a
            WHERE (:texto IS NULL OR c.name ILIKE %:texto%)
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    Page<CourseSummaryResponse> findSummariesByFilter(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.common.ChartItemResponse(c.name, COUNT(l))
            FROM Course c
            JOIN c.readers a
            JOIN Loan l ON l.reader = a
            GROUP BY c.name
            HAVING COUNT(l) > 0
            """)
    List<ChartItemResponse> findTotalEmprestimosPorCurso();
}
