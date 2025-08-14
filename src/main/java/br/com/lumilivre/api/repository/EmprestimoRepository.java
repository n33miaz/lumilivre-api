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

import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;

@Repository
public interface EmprestimoRepository extends JpaRepository<EmprestimoModel, Integer> {

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
            Pageable pageable
        );
    }
