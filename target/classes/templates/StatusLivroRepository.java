package br.com.lumilivre.api.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.lumilivre.api.model.StatusLivroModel;

public interface StatusLivroRepository extends CrudRepository<StatusLivroModel, Long> {
    
}
