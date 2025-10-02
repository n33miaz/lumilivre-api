package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.ModuloModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ModuloRepository extends JpaRepository<ModuloModel, Integer> {
    // busca todos os modulos ordenados por nome
    List<ModuloModel> findAllByOrderByNomeAsc();
}