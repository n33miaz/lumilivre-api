package br.com.lumilivre.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.CursoModel;

@Repository
public interface CursoRepository extends CrudRepository<CursoModel, Long> {

        CursoModel findById(Long id);

}
