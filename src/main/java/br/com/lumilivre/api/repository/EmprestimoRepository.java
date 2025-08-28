package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.data.EmprestimoResponseDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;

@Repository
public interface EmprestimoRepository extends JpaRepository<EmprestimoModel, Integer> {

    // Substituindo findByAluno_MatriculaAndConcluido
    List<EmprestimoModel> findByAluno_MatriculaAndStatusEmprestimo(String matricula, StatusEmprestimo statusEmprestimo);

    boolean existsByExemplarTomboAndStatusEmprestimo(String tombo, StatusEmprestimo status);

    Optional<EmprestimoModel> findById(Integer id);

    List<EmprestimoModel> findByStatusEmprestimo(StatusEmprestimo status);

    List<EmprestimoModel> findByStatusEmprestimoAndDataDevolucaoBefore(StatusEmprestimo status, LocalDateTime now);

    List<EmprestimoModel> findByStatusEmprestimoAndDataDevolucaoGreaterThanEqual(StatusEmprestimo status,
            LocalDateTime now);

    List<EmprestimoModel> findByAluno_Matricula(String matricula);

    List<EmprestimoModel> findByDataEmprestimoGreaterThanEqual(LocalDateTime dataInicio);

    List<EmprestimoModel> findByExemplar_Tombo(String tombo);

    List<EmprestimoModel> findByDataEmprestimoBetween(LocalDateTime inicio, LocalDateTime fim);

    List<EmprestimoModel> findByDataDevolucaoBetween(LocalDateTime inicio, LocalDateTime fim);

    long countByAlunoMatriculaAndStatusEmprestimo(String matricula, StatusEmprestimo status);

    @Query("""
                SELECT e FROM EmprestimoModel e
                WHERE LOWER(e.exemplar.tombo) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(e.aluno.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(CAST(e.penalidade AS string)) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(CAST(e.statusEmprestimo AS string)) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR STR(e.dataEmprestimo) LIKE CONCAT('%', :texto, '%')
                   OR STR(e.dataDevolucao) LIKE CONCAT('%', :texto, '%')
            """)
    Page<EmprestimoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                SELECT e FROM EmprestimoModel e
                WHERE (:statusEmprestimo IS NULL OR e.statusEmprestimo = :statusEmprestimo)
                  AND (:tombo IS NULL OR LOWER(e.exemplar.tombo) LIKE LOWER(CONCAT('%', :tombo, '%')))
                  AND (:livroNome IS NULL OR LOWER(e.exemplar.livro.nome) LIKE LOWER(CONCAT('%', :livroNome, '%')))
                  AND (:alunoNome IS NULL OR LOWER(e.aluno.nome) LIKE LOWER(CONCAT('%', :alunoNome, '%')))
                  AND (:dataEmprestimo IS NULL OR STR(e.dataEmprestimo) LIKE CONCAT('%', :dataEmprestimo, '%'))
                  AND (:dataDevolucao IS NULL OR STR(e.dataDevolucao) LIKE CONCAT('%', :dataDevolucao, '%'))
            """)
    Page<EmprestimoModel> buscarAvancado(
            @Param("statusEmprestimo") StatusEmprestimo statusEmprestimo,
            @Param("tombo") String tombo,
            @Param("livroNome") String livroNome,
            @Param("alunoNome") String alunoNome,
            @Param("dataEmprestimo") String dataEmprestimo,
            @Param("dataDevolucao") String dataDevolucao,
            Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.data.EmprestimoResponseDTO(
                    e.id, e.dataEmprestimo, e.dataDevolucao, e.statusEmprestimo, e.penalidade, e.exemplar.livro.nome
                )
                FROM EmprestimoModel e
                WHERE e.aluno.matricula = :matricula
                  AND e.statusEmprestimo = 'ATIVO'
            """)
    List<EmprestimoResponseDTO> findEmprestimosAtivos(@Param("matricula") String matricula);

    @Query("""
                SELECT new br.com.lumilivre.api.data.EmprestimoResponseDTO(
                    e.id, e.dataEmprestimo, e.dataDevolucao, e.statusEmprestimo, e.penalidade, e.exemplar.livro.nome
                )
                FROM EmprestimoModel e
                WHERE e.aluno.matricula = :matricula
                  AND e.statusEmprestimo = 'CONCLUIDO'
            """)
    List<EmprestimoResponseDTO> findHistoricoEmprestimos(@Param("matricula") String matricula);

}
