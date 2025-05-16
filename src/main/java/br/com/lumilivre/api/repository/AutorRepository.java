package br.com.lumilivre.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AutorModel;

@Repository
public interface AutorRepository extends CrudRepository<AutorModel, String> {
}
