package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import br.com.lumilivre.api.model.ExemplarModel;

@Repository
public interface ExemplarRepository extends JpaRepository<ExemplarModel, String> {

    Optional<ExemplarModel> findByTombo(String tombo);

    boolean existsByTombo(String tombo);

    List<ExemplarModel> findByLivroId(Long livroId);

    @Query("SELECT e FROM ExemplarModel e WHERE e.livro.isbn = :isbn")
    List<ExemplarModel> findAllByLivroIsbn(@Param("isbn") String isbn);

    Long countByLivroId(Long livroId);

    void deleteAllByLivroId(Long livroId);

    List<ExemplarModel> findAllByLivroId(Long livroId);
}