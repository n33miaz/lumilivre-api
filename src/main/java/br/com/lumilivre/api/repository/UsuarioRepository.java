package br.com.lumilivre.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.UsuarioModel;

public interface UsuarioRepository extends JpaRepository<UsuarioModel, Integer > {
    boolean existsByEmail(String email);
    boolean existsByAluno(AlunoModel aluno);

}
