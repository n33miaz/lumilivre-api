package br.com.lumilivre.api.repository;
import org.springframework.data.repository.CrudRepository;

import br.com.lumilivre.api.model.GeneroModel;


public interface GeneroRepository extends CrudRepository<GeneroModel, Long>{
    
}
