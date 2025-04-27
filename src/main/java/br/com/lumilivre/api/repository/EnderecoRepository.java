package br.com.lumilivre.api.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.lumilivre.api.model.EnderecoModel;

public interface EnderecoRepository extends CrudRepository<EnderecoModel, Long> {
    
}
