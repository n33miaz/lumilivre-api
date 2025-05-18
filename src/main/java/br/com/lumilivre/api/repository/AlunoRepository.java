package br.com.lumilivre.api.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.lumilivre.api.model.AlunoModel;

public interface AlunoRepository extends CrudRepository<AlunoModel, String> {

}
