package br.com.lumilivre.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.CursoModel;

@Repository
public interface CursoRepository extends JpaRepository<CursoModel, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

}
