package br.com.lumilivre.api.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import br.com.lumilivre.api.model.EnderecoModel;

public interface EnderecoRepository extends CrudRepository<EnderecoModel, Long> {
    Optional<EnderecoModel> findByCep(String cep);
}
