package br.com.lumilivre.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.AutorModel;

public interface AlunoRepository extends JpaRepository<AlunoModel, String> {
    Optional<AlunoModel> findByMatricula(String matricula);
    Optional<AlunoModel> findByNome(String nome);
    Optional<AlunoModel> findByCpf(String cpf);
	AutorModel findByNomeIgnoreCase(String nome);
}
