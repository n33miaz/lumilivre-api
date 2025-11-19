package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.ModuloModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloRepository extends JpaRepository<ModuloModel, Integer> {
}