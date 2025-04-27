package br.com.lumilivre.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AlunoModel;

@Repository
public interface AlunoRepository extends CrudRepository<AlunoModel, String> {

    AlunoModel findByMatricula(String matricula);
}
