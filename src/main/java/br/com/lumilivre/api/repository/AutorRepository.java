package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.AutorModel;

@Repository
public interface AutorRepository extends JpaRepository<AutorModel, String> {
    Optional<AutorModel> findByNome(String nome);

    AutorModel findByCodigo(String codigo);

    @Query("""
                SELECT a FROM AutorModel a
                WHERE LOWER(a.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(a.pseudonimo) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR STR(a.nacionalidade) LIKE CONCAT('%', :texto, '%')
            """)
    Page<AutorModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                SELECT a FROM AutorModel a
                WHERE (:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
                  AND (:pseudonimo IS NULL OR a.pseudonimo = :pseudonimo)
                  AND (:nacionalidade IS NULL OR a.nacionalidade = :nacionalidade)
            """)
    Page<AutorModel> buscarAvancado(
            @Param("nome") String nome,
            @Param("pseudonimo") String pseudonimo,
            @Param("nacionalidade") String nacionalidade,
            Pageable pageable);
}
