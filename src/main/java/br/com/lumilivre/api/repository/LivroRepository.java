package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Repository;


import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;

@Repository
public interface LivroRepository extends JpaRepository<LivroModel, String> {
    Optional<LivroModel> findById(String isbn);
    
    Optional<LivroModel> findByIsbn(String isbn);


    boolean existsByIsbn(String isbn);

    void deleteByIsbn(String isbn);

    // Busca geral
    @Query("""
        SELECT l FROM LivroModel l
        WHERE LOWER(l.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(l.sinopse) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(l.autor.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(l.genero.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :texto, '%'))
    """)
    Page<LivroModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    // Busca avançada com todos os filtros
    @Query("""
        SELECT l FROM LivroModel l
        WHERE (:nome IS NULL OR LOWER(l.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
          AND (:isbn IS NULL OR l.isbn = :isbn)
          AND (:autor IS NULL OR LOWER(l.autor.nome) LIKE LOWER(CONCAT('%', :autor, '%')))
          AND (:genero IS NULL OR LOWER(l.genero.nome) LIKE LOWER(CONCAT('%', :genero, '%')))
          AND (:editora IS NULL OR LOWER(l.editora) LIKE LOWER(CONCAT('%', :editora, '%')))
    """)
    Page<LivroModel> buscarAvancado(
        @Param("nome") String nome,
        @Param("isbn") String isbn,
        @Param("autor") String autor,
        @Param("genero") String genero,
        @Param("editora") String editora,
        Pageable pageable
    );
}