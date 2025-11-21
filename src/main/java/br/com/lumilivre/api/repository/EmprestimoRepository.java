package br.com.lumilivre.api.repository;

import java.time.LocalDate;
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
                    e.status_emprestimo as statusEmprestimo,
                    l.nome as livroNome,
                    ex.tombo as livroTombo,
                    a.nome_completo as nomeAluno,
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
                    e.statusEmprestimo,
                    l.nome,
                    ex.tombo,
                    a.nomeCompleto,
                    c.nome,
                    e.dataEmprestimo,
                    e.dataDevolucao
                )
                FROM EmprestimoModel e
                JOIN e.aluno a
                JOIN e.exemplar ex
                JOIN ex.livro l
                JOIN a.curso c
                WHERE (:statusEmprestimo IS NULL OR e.statusEmprestimo = :statusEmprestimo)
                AND (:tombo IS NULL OR ex.tombo ILIKE :tombo)
                AND (:livroNome IS NULL OR l.nome ILIKE :livroNome)
                AND (:alunoNomeCompleto IS NULL OR a.nomeCompleto ILIKE :alunoNomeCompleto)
                AND (:dataEmprestimoInicio IS NULL OR e.dataEmprestimo >= :dataEmprestimoInicio)
                AND (:dataEmprestimoFim IS NULL OR e.dataEmprestimo <= :dataEmprestimoFim)
                AND (:dataDevolucaoInicio IS NULL OR e.dataDevolucao >= :dataDevolucaoInicio)
                AND (:dataDevolucaoFim IS NULL OR e.dataDevolucao <= :dataDevolucaoFim)
            """)
    Page<EmprestimoListagemResponse> buscarAvancado(
            @Param("statusEmprestimo") StatusEmprestimo statusEmprestimo,
            @Param("tombo") String tombo,
            @Param("livroNome") String livroNome,
            @Param("alunoNomeCompleto") String alunoNomeCompleto,
            @Param("dataEmprestimoInicio") LocalDateTime dataEmprestimoInicio,
            @Param("dataEmprestimoFim") LocalDateTime dataEmprestimoFim,
            @Param("dataDevolucaoInicio") LocalDateTime dataDevolucaoInicio,
            @Param("dataDevolucaoFim") LocalDateTime dataDevolucaoFim,
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
                    e.statusEmprestimo,
                    l.nome,
                    ex.tombo,
                    a.nomeCompleto,
                    a.curso.nome,
                    e.dataEmprestimo,
                    e.dataDevolucao
                )
                FROM EmprestimoModel e
                JOIN e.exemplar ex
                JOIN ex.livro l
                JOIN e.aluno a
                ORDER BY e.statusEmprestimo
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
                SELECT e FROM EmprestimoModel e
                JOIN FETCH e.aluno a
                JOIN FETCH a.curso
                LEFT JOIN FETCH a.modulo
                JOIN FETCH e.exemplar ex
                JOIN FETCH ex.livro
                WHERE (:inicio IS NULL OR e.dataEmprestimo >= :inicio)
                  AND (:fim IS NULL OR e.dataEmprestimo <= :fim)
                  AND (:status IS NULL OR e.statusEmprestimo = :status)
                  AND (:matriculaAluno IS NULL OR a.matricula = :matriculaAluno)
                  AND (:idCurso IS NULL OR a.curso.id = :idCurso)
                  AND (:idModulo IS NULL OR a.modulo.id = :idModulo)
                  AND (:isbnOuTombo IS NULL OR ex.tombo = :isbnOuTombo OR ex.livro.isbn = :isbnOuTombo)
            """)
    List<EmprestimoModel> findForReport(
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
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
                CAST(e.dataDevolucao AS LocalDate),
                e.statusEmprestimo
            )
            FROM EmprestimoModel e
            JOIN e.aluno a
            JOIN e.exemplar ex
            JOIN ex.livro l
            WHERE e.statusEmprestimo = br.com.lumilivre.api.enums.StatusEmprestimo.ATRASADO
            ORDER BY e.dataDevolucao ASC
            """)
    List<EmprestimoAtivoResponse> findApenasAtrasadosDTO();
}