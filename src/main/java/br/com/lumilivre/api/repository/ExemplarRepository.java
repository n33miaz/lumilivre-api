package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.model.ExemplarModel;

@Repository
public interface ExemplarRepository extends JpaRepository<ExemplarModel, String> {

  Optional<ExemplarModel> findByTombo(String tombo);

  boolean existsByTombo(String tombo);

  List<ExemplarModel> findByLivroId(Long livroId);

  @Query("SELECT e FROM ExemplarModel e WHERE e.livro.isbn = :isbn")
  List<ExemplarModel> findAllByLivroIsbn(@Param("isbn") String isbn);

  @Query("SELECT e FROM ExemplarModel e JOIN FETCH e.livro l LEFT JOIN FETCH l.generos WHERE l.id = :livroId")
  List<ExemplarModel> findAllByLivroIdWithDetails(@Param("livroId") Long livroId);

  Long countByLivroId(Long livroId);

  void deleteAllByLivroId(Long livroId);

  List<ExemplarModel> findAllByLivroId(Long livroId);

  @Query("SELECT COUNT(e) FROM ExemplarModel e WHERE e.livro.id = :livroId AND e.status_livro = :status")
  long countExemplaresByStatus(@Param("livroId") Long livroId, @Param("status") StatusLivro status);

  @Query("SELECT e FROM ExemplarModel e WHERE e.livro.id = :livroId AND e.status_livro = :status")
  List<ExemplarModel> findExemplaresPorLivroEStatus(@Param("livroId") Long livroId,
      @Param("status") StatusLivro status);

  @Query("""
          SELECT ex FROM ExemplarModel ex
          JOIN FETCH ex.livro l
          WHERE (:status IS NULL OR ex.status_livro = :status)
            AND (:isbnOuTombo IS NULL
                 OR ex.tombo ILIKE :isbnOuTombo
                 OR l.isbn ILIKE :isbnOuTombo
                 OR l.nome ILIKE :isbnOuTombo)
            AND (cast(:inicio as date) IS NULL OR ex.dataInclusao >= :inicio)
            AND (cast(:fim as date) IS NULL OR ex.dataInclusao <= :fim)
          ORDER BY l.nome, ex.tombo
      """)
  List<ExemplarModel> findForReport(
      @Param("status") StatusLivro status,
      @Param("isbnOuTombo") String isbnOuTombo,
      @Param("inicio") LocalDateTime inicio,
      @Param("fim") LocalDateTime fim);

  default Optional<ExemplarModel> findFirstDisponivel(Long livroId, StatusLivro status) {
    List<ExemplarModel> lista = findExemplaresPorLivroEStatus(livroId, status);
    return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
  }
}