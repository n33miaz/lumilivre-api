package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.dto.modulo.ModuloResumoResponse;
import br.com.lumilivre.api.model.ModuloModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import java.util.List;

public interface ModuloRepository extends JpaRepository<ModuloModel, Integer> {

    @Cacheable("modulos")
    @Override
    @NonNull
    List<ModuloModel> findAll();

    boolean existsByNomeIgnoreCase(String nome);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.modulo.ModuloResumoResponse(m.id, m.nome, COUNT(a))
            FROM ModuloModel m
            LEFT JOIN m.alunos a
            WHERE (:texto IS NULL OR m.nome ILIKE CONCAT('%', :texto, '%'))
            GROUP BY m.id, m.nome
            ORDER BY m.nome
            """)
    Page<ModuloResumoResponse> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);
}