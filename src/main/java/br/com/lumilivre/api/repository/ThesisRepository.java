package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.Thesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ThesisRepository extends JpaRepository<Thesis, UUID> {

    @Query("SELECT t FROM Thesis t JOIN FETCH t.course")
    List<Thesis> findAllWithCourse();

    @Query("SELECT t FROM Thesis t " +
            "JOIN FETCH t.course c " +
            "WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(t.authors) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<Thesis> searchByText(@Param("text") String text);

    @Query("SELECT t FROM Thesis t " +
            "JOIN FETCH t.course c " +
            "WHERE (:courseId IS NULL OR c.id = :courseId) " +
            "AND (:semester IS NULL OR t.completionSemester = :semester) " +
            "AND (:year IS NULL OR t.completionYear = :year)")
    List<Thesis> searchAdvanced(
            @Param("courseId") Integer courseId,
            @Param("semester") String semester,
            @Param("year") Integer year);
}
