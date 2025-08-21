package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.LivroModel;

public interface AlunoRepository extends JpaRepository<AlunoModel, String> {
    Optional<AlunoModel> findByMatricula(String matricula);
    Optional<AlunoModel> findByCpf(String cpf);
Optional<AlunoModel> findByNomeIgnoreCase(String nome);

    
    @Query("""
        SELECT a FROM AlunoModel a
        WHERE LOWER(a.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(a.matricula) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR STR(a.dataNascimento) LIKE CONCAT('%', :texto, '%')
           OR LOWER(a.curso.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
    """)
    Page<AlunoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
        SELECT a FROM AlunoModel a
        WHERE (:nome IS NULL OR LOWER(a.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
          AND (:matricula IS NULL OR a.matricula = :matricula)
          AND (:dataNascimento IS NULL OR a.dataNascimento = :dataNascimento)
          AND (:cursoNome IS NULL OR LOWER(a.curso.nome) LIKE LOWER(CONCAT('%', :cursoNome, '%')))
    """)
    Page<AlunoModel> buscarAvancado(
        @Param("nome") String nome,
        @Param("matricula") String matricula,
        @Param("dataNascimento") LocalDate dataNascimento,
        @Param("cursoNome") String cursoNome,
        Pageable pageable
    );
}
