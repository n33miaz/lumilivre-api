package br.com.lumilivre.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.LivroModel;

@Repository
public interface LivroRepository extends JpaRepository<LivroModel, String> {
    Optional<LivroModel> findByIsbn(String isbn); 
    boolean existsByIsbn(String isbn);

}