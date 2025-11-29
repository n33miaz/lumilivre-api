package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoAtivoResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoListagemResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoDashboardResponse;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;

@Repository
public interface EmprestimoRepository extends JpaRepository<EmprestimoModel, Integer> {

    List<EmprestimoModel> findByStatusEmprestimoIn(List<StatusEmprestimo> statuses);

    long countByStatusEmprestimoIn(List<StatusEmprestimo> statuses);

    List<EmprestimoModel> findByStatusEmprestimoAndDataDevolucaoBefore(StatusEmprestimo status, LocalDateTime now);

    List<EmprestimoModel> findByStatusEmprestimoAndDataDevolucaoGreaterThanEqual(StatusEmprestimo status,
            LocalDateTime now);

    List<EmprestimoModel> findByAluno_Matricula(String matricula);

    List<EmprestimoModel> findByDataEmprestimoGreaterThanEqual(LocalDateTime dataInicio);

    List<EmprestimoModel> findByExemplar_Tombo(String tombo);

    List<EmprestimoModel> findByDataEmprestimoBetween(LocalDateTime inicio, LocalDateTime fim);

    List<EmprestimoModel> findByDataDevolucaoBetween(LocalDateTime inicio, LocalDateTime fim);

    boolean existsByExemplarTomboAndStatusEmprestimo(String tombo, StatusEmprestimo status);

    boolean existsByExemplarTomboAndStatusEmprestimoIn(String tombo, List<StatusEmprestimo> statuses);

    long countByAlunoMatriculaAndStatusEmprestimo(String matricula, StatusEmprestimo status);

    List<EmprestimoModel> findByStatusEmprestimo(StatusEmprestimo atrasado);

    @Query(value = """
                SELECT
                    e.id as id,
                    e.status_emprestimo as statusEmprestimo,
                    l.nome as livroNome,
                    ex.tombo as livroTombo,
                    a.nome_completo as nomeAluno,
                    a.matricula as matriculaAluno,
                    c.nome as curso,
                    e.data_emprestimo as dataEmprestimo,
                    e.data_devolucao as dataDevolucao
                FROM emprestimo e
                JOIN aluno a ON e.aluno_matricula = a.matricula
                JOIN exemplar ex ON e.exemplar_tombo = ex.tombo
                JOIN livro l ON ex.livro_id = l.id
                JOIN curso c ON a.curso_id = c.id
                WHERE e.texto_busca @@ plainto_tsquery('portuguese', :texto)
            """, countQuery = """
                SELECT count(*) FROM emprestimo e
                WHERE e.texto_busca @@ plainto_tsquery('portuguese', :texto)
            """, nativeQuery = true)
    Page<EmprestimoListagemResponse> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoListagemResponse(
                    e.id,
                    e.statusEmprestimo,
                    l.nome,
                    ex.tombo,
                    a.nomeCompleto,
                    a.matricula,
                    c.nome,
                    e.dataEmprestimo,
                    e.dataDevolucao
                )
                FROM EmprestimoModel e
                JOIN e.aluno a
                JOIN e.exemplar ex
                JOIN ex.livro l
                JOIN a.curso c
                WHERE
                (
                    (:statusEmprestimo IS NULL) OR
                    (:statusEmprestimo = 'CONCLUIDO' AND e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.CONCLUIDO) OR
                    (:statusEmprestimo = 'ATRASADO' AND (e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.ATRASADO OR (e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.ATIVO AND e.dataDevolucao < :now))) OR
                    (:statusEmprestimo = 'ATIVO' AND (e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.ATIVO AND e.dataDevolucao >= :now))
                )
                AND (:tombo IS NULL OR ex.tombo ILIKE :tombo)
                AND (:livroNome IS NULL OR l.nome ILIKE :livroNome)
                AND (:alunoNomeCompleto IS NULL OR a.nomeCompleto ILIKE :alunoNomeCompleto)
                AND (cast(:dataEmprestimoInicio as timestamp) IS NULL OR e.dataEmprestimo >= :dataEmprestimoInicio)
                AND (cast(:dataEmprestimoFim as timestamp) IS NULL OR e.dataEmprestimo <= :dataEmprestimoFim)
                AND (cast(:dataDevolucaoInicio as timestamp) IS NULL OR e.dataDevolucao >= :dataDevolucaoInicio)
                AND (cast(:dataDevolucaoFim as timestamp) IS NULL OR e.dataDevolucao <= :dataDevolucaoFim)
            """)
    Page<EmprestimoListagemResponse> buscarAvancado(
            @Param("statusEmprestimo") String statusEmprestimo,
            @Param("tombo") String tombo,
            @Param("livroNome") String livroNome,
            @Param("alunoNomeCompleto") String alunoNomeCompleto,
            @Param("dataEmprestimoInicio") LocalDateTime dataEmprestimoInicio,
            @Param("dataEmprestimoFim") LocalDateTime dataEmprestimoFim,
            @Param("dataDevolucaoInicio") LocalDateTime dataDevolucaoInicio,
            @Param("dataDevolucaoFim") LocalDateTime dataDevolucaoFim,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse(
                    e.id, e.dataEmprestimo, e.dataDevolucao, e.statusEmprestimo, e.penalidade, e.exemplar.livro.nome
                )
                FROM EmprestimoModel e
                WHERE e.aluno.matricula = :matricula
                  AND e.statusEmprestimo = 'ATIVO'
            """)
    List<EmprestimoResponse> findEmprestimosAtivos(@Param("matricula") String matricula);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse(
                    e.id, e.dataEmprestimo, e.dataDevolucao, e.statusEmprestimo, e.penalidade, e.exemplar.livro.nome
                )
                FROM EmprestimoModel e
                WHERE e.aluno.matricula = :matricula
                  AND e.statusEmprestimo = 'CONCLUIDO'
            """)
    List<EmprestimoResponse> findHistoricoEmprestimos(@Param("matricula") String matricula);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoListagemResponse(
                    e.id,
                    e.statusEmprestimo,
                    l.nome,
                    ex.tombo,
                    a.nomeCompleto,
                    a.matricula,
                    a.curso.nome,
                    e.dataEmprestimo,
                    e.dataDevolucao
                )
                FROM EmprestimoModel e
                JOIN e.exemplar ex
                JOIN ex.livro l
                JOIN e.aluno a
            """)
    Page<EmprestimoListagemResponse> findEmprestimoParaListaAdmin(Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoDashboardResponse(
                    livro.nome,
                    aluno.nomeCompleto,
                    emprestimo.dataDevolucao,
                    emprestimo.statusEmprestimo
                )
                FROM EmprestimoModel emprestimo
                JOIN emprestimo.exemplar exemplar
                JOIN exemplar.livro livro
                JOIN emprestimo.aluno aluno
                WHERE emprestimo.statusEmprestimo IN (
                    br.com.lumilivre.api.enums.StatusEmprestimo.ATIVO,
                    br.com.lumilivre.api.enums.StatusEmprestimo.ATRASADO
                )
                ORDER BY emprestimo.dataDevolucao ASC
            """)
    List<EmprestimoDashboardResponse> findEmprestimosAtivosEAtrasados();

    @Query("""
            SELECT DISTINCT e FROM EmprestimoModel e
            LEFT JOIN FETCH e.aluno a
            LEFT JOIN FETCH a.curso
            LEFT JOIN FETCH a.modulo
            LEFT JOIN FETCH e.exemplar ex
            LEFT JOIN FETCH ex.livro l
            WHERE (cast(:inicio as timestamp) IS NULL OR e.dataEmprestimo >= :inicio)
              AND (cast(:fim as timestamp) IS NULL OR e.dataEmprestimo <= :fim)
              AND (:status IS NULL OR e.statusEmprestimo = :status)
              AND (:matriculaAluno IS NULL OR a.matricula ILIKE :matriculaAluno OR a.nomeCompleto ILIKE :matriculaAluno)
              AND (cast(:idCurso as integer) IS NULL OR a.curso.id = :idCurso)
              AND (cast(:idModulo as integer) IS NULL OR a.modulo.id = :idModulo)
              AND (
                    :isbnOuTombo IS NULL
                    OR ex.tombo ILIKE :isbnOuTombo
                    OR l.isbn ILIKE :isbnOuTombo
                    OR l.nome ILIKE :isbnOuTombo
              )
            ORDER BY e.dataEmprestimo DESC
            """)
    List<EmprestimoModel> findForReport(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusEmprestimo status,
            @Param("matriculaAluno") String matriculaAluno,
            @Param("idCurso") Integer idCurso,
            @Param("isbnOuTombo") String isbnOuTombo,
            @Param("idModulo") Integer idModulo);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoAtivoResponse(
                e.id,
                l.nome,
                a.nomeCompleto,
                a.matricula,
                ex.tombo,
                CAST(e.dataEmprestimo AS LocalDate),
                CAST(e.dataDevolucao AS LocalDate),
                e.statusEmprestimo
            )
            FROM EmprestimoModel e
            JOIN e.aluno a
            JOIN e.exemplar ex
            JOIN ex.livro l
            WHERE e.statusEmprestimo IN (br.com.lumilivre.api.enums.StatusEmprestimo.ATIVO, br.com.lumilivre.api.enums.StatusEmprestimo.ATRASADO)
            ORDER BY e.dataDevolucao ASC
            """)
    List<EmprestimoAtivoResponse> findAtivosEAtrasadosDTO();

    @Query("""
            SELECT new br.com.lumilivre.api.dto.emprestimo.EmprestimoAtivoResponse(
                e.id,
                l.nome,
                a.nomeCompleto,
                a.matricula,
                ex.tombo,
                CAST(e.dataEmprestimo AS LocalDate),
                CAST(e.dataDevolucao AS LocalDate),
                e.statusEmprestimo
            )
            FROM EmprestimoModel e
            JOIN e.aluno a
            JOIN e.exemplar ex
            JOIN ex.livro l
            WHERE e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.ATRASADO
               OR (e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.ATIVO AND e.dataDevolucao < CURRENT_TIMESTAMP)
            ORDER BY e.dataDevolucao ASC
            """)
    List<EmprestimoAtivoResponse> findApenasAtrasadosDTO();
}