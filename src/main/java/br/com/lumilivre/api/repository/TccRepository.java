package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.TccModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TccRepository extends JpaRepository<TccModel, Long> {

    @Query("SELECT t FROM TccModel t JOIN FETCH t.curso")
    List<TccModel> findAllCompleto();

    @Query("SELECT t FROM TccModel t " +
            "JOIN FETCH t.curso c " +
            "WHERE LOWER(t.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(t.alunos) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<TccModel> buscarPorTexto(@Param("texto") String texto);

    @Query("SELECT t FROM TccModel t " +
            "JOIN FETCH t.curso c " +
            "WHERE (:cursoId IS NULL OR c.id = :cursoId) " +
            "AND (:semestre IS NULL OR t.semestreConclusao = :semestre) " +
            "AND (:ano IS NULL OR t.anoConclusao = :ano)")
    List<TccModel> buscarAvancado(
            @Param("cursoId") Integer cursoId,
            @Param("semestre") String semestre,
            @Param("ano") String ano);
}