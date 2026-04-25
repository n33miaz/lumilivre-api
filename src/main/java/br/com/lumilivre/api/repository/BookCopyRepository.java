package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.model.BookCopy;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, UUID> {

    Optional<BookCopy> findByCopyCode(String copyCode);

    boolean existsByCopyCode(String copyCode);

    List<BookCopy> findByBook_Id(UUID bookId);

    @Query("SELECT e FROM BookCopy e WHERE e.book.isbn = :isbn")
    List<BookCopy> findAllByBookIsbn(@Param("isbn") String isbn);

    @Query("SELECT e FROM BookCopy e JOIN FETCH e.book b LEFT JOIN FETCH b.genres WHERE b.id = :bookId")
    List<BookCopy> findAllByBookIdWithDetails(@Param("bookId") UUID bookId);

    long countByBook_Id(UUID bookId);

    void deleteAllByBook_Id(UUID bookId);

    List<BookCopy> findAllByBook_Id(UUID bookId);

    @Query("SELECT COUNT(e) FROM BookCopy e WHERE e.book.id = :bookId AND e.status = :status")
    long countByBookIdAndStatus(@Param("bookId") UUID bookId, @Param("status") BookCopyStatus status);

    @Query("SELECT e FROM BookCopy e WHERE e.book.id = :bookId AND e.status = :status")
    List<BookCopy> findByBookIdAndStatus(@Param("bookId") UUID bookId, @Param("status") BookCopyStatus status);

    @Query("""
          SELECT ex FROM BookCopy ex
          JOIN FETCH ex.book l
          WHERE (:status IS NULL OR ex.status = :status)
            AND (:isbnOuTombo IS NULL
                 OR ex.copyCode ILIKE :isbnOuTombo
                 OR l.isbn ILIKE :isbnOuTombo
                 OR l.title ILIKE :isbnOuTombo)
            AND (cast(:inicio as timestamp) IS NULL OR ex.createdAt >= :inicio)
            AND (cast(:fim as timestamp) IS NULL OR ex.createdAt <= :fim)
          ORDER BY l.title, ex.copyCode
      """)
    List<BookCopy> findForReport(
            @Param("status") BookCopyStatus status,
            @Param("isbnOuTombo") String isbnOuTombo,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    default Optional<BookCopy> findFirstAvailable(UUID bookId) {
        List<BookCopy> lista = findByBookIdAndStatus(bookId, BookCopyStatus.AVAILABLE);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }
}
