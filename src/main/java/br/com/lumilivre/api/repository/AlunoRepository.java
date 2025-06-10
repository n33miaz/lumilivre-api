package br.com.lumilivre.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.AlunoModel;

public interface AlunoRepository extends JpaRepository<AlunoModel, String> {
    static Optional<AlunoModel> findByMatricula(String matricula) {
		// TODO Auto-generated method stub
		return null;
	}

}
