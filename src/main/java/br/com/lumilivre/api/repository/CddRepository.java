package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.CddModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CddRepository extends JpaRepository<CddModel, String> {
    
}