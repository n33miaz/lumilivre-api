package br.com.lumilivre.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.AlunoModel;

public interface AlunoRepository extends JpaRepository<AlunoModel, String> {

}
