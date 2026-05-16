package br.com.lumilivre.api.repository;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import br.com.lumilivre.api.dto.academicmodule.AcademicModuleSummaryResponse;
import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.model.AcademicModule;

public interface AcademicModuleRepository extends JpaRepository<AcademicModule, Integer> {

    @Cacheable("modulos")
    @Override
    @NonNull
    List<AcademicModule> findAll();

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.academicmodule.AcademicModuleSummaryResponse(m.id, m.name, COUNT(a))
            FROM AcademicModule m
            LEFT JOIN m.students a
            WHERE (:texto IS NULL OR m.name ILIKE %:texto%)
            GROUP BY m.id, m.name
            ORDER BY m.name
            """)
    Page<AcademicModuleSummaryResponse> findSummaries(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.common.ChartItemResponse(m.name, COUNT(l))
            FROM AcademicModule m
            JOIN m.students a
            JOIN Loan l ON l.student = a
            GROUP BY m.name
            HAVING COUNT(l) > 0
            """)
    List<ChartItemResponse> findTotalEmprestimosPorModulo();
}
