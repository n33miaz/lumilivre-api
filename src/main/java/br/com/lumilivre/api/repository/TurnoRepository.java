package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.TurnoModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurnoRepository extends JpaRepository<TurnoModel, Integer> {
}