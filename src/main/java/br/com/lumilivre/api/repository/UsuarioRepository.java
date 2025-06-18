package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.UsuarioModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<UsuarioModel, Integer> {    
}
