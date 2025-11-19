package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.GeneroModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GeneroRepository extends JpaRepository<GeneroModel, Integer> {

    @Cacheable("generos")
    @Override
    @NonNull
    List<GeneroModel> findAll();

    Optional<GeneroModel> findByNomeIgnoreCase(String nome);

    Set<GeneroModel> findByNomeIn(Set<String> nomes);

    @Query(value = "SELECT g.* FROM genero g " +
            "JOIN genero_cdd gc ON g.id = gc.genero_id " +
            "WHERE gc.cdd_codigo = :cddCodigo", nativeQuery = true)
    Set<GeneroModel> findAllByCddCodigo(@Param("cddCodigo") String cddCodigo);
}