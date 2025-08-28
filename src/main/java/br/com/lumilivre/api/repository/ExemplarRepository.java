package br.com.lumilivre.api.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import br.com.lumilivre.api.model.ExemplarModel;

@Repository
public interface ExemplarRepository extends JpaRepository<ExemplarModel, String> {
    Optional<ExemplarModel> findByTombo(String tombo);

    boolean existsByTombo(String tombo);

    List<ExemplarModel> findAllByLivroIsbn(String isbn);

    void deleteAllByLivroIsbn(String isbn);

    @Query("SELECT COUNT(e) FROM ExemplarModel e WHERE e.livro.isbn = :isbn")
    Long contarExemplaresPorLivro(String isbn);

}