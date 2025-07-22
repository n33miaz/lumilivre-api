package br.com.lumilivre.api.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.GeneroModel;


public interface GeneroRepository extends JpaRepository<GeneroModel, Integer>{
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);
    GeneroModel findByNomeIgnoreCase(String nome);

}
