package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.dto.modulo.ModuloResumoResponse;
import br.com.lumilivre.api.model.AcademicModule;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import java.util.List;

public interface AcademicModuleRepository extends JpaRepository<AcademicModule, Integer> {

    @Cacheable("modulos")
    @Override
    @NonNull
    List<AcademicModule> findAll();

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.modulo.ModuloResumoResponse(m.id, m.name, COUNT(a))
            FROM AcademicModule m
            LEFT JOIN m.alunos a
            WHERE (:texto IS NULL OR m.name ILIKE %:texto%)
            GROUP BY m.id, m.name
            ORDER BY m.name
            """)
    Page<ModuloResumoResponse> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT new br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse(m.name, SUM(a.emprestimosCount)) FROM AcademicModule m JOIN m.alunos a GROUP BY m.name HAVING SUM(a.emprestimosCount) > 0")
    List<br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse> findTotalEmprestimosPorModulo();
}
