package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.TccModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TccRepository extends JpaRepository<TccModel, Long> {
}
