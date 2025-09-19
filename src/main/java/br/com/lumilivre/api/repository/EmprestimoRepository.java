package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.data.EmprestimoResponseDTO;
import br.com.lumilivre.api.data.ListaEmprestimoDTO;
import br.com.lumilivre.api.data.ListaEmprestimoDashboardDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;

@Repository
public interface EmprestimoRepository extends JpaRepository<EmprestimoModel, Integer> {

    boolean existsByExemplarTomboAndStatusEmprestimo(String tombo, StatusEmprestimo status);

    // busca e conta ATIVO e ATRASADO
    List<EmprestimoModel> findByStatusEmprestimoIn(List<StatusEmprestimo> statuses);

    long countByStatusEmprestimoIn(List<StatusEmprestimo> statuses);

    long countByAlunoMatriculaAndStatusEmprestimo(String matricula, StatusEmprestimo status);

    // Busca por texto genérica
    @Query(value = """
                SELECT *
                FROM emprestimo e
                WHERE e.texto_busca @@ plainto_tsquery('portuguese', :texto)
            """, nativeQuery = true)
    Page<EmprestimoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    // Busca avançada agora com LocalDateTime
    @Query("""
                SELECT e FROM EmprestimoModel e
                WHERE (:statusEmprestimo IS NULL OR e.statusEmprestimo = :statusEmprestimo)
                  AND (:tombo IS NULL OR LOWER(e.exemplar.tombo) LIKE LOWER(CONCAT('%', :tombo, '%')))
                  AND (:livroNome IS NULL OR LOWER(e.exemplar.livro.nome) LIKE LOWER(CONCAT('%', :livroNome, '%')))
                  AND (:alunoNome IS NULL OR LOWER(e.aluno.nome) LIKE LOWER(CONCAT('%', :alunoNome, '%')))
                  AND (:alunoNome IS NULL OR LOWER(e.aluno.nome) LIKE LOWER(CONCAT('%', :alunoNome, '%')))
                  AND (:dataEmprestimoInicio IS NULL OR e.dataEmprestimo >= :dataEmprestimoInicio)
                  AND (:dataEmprestimoFim IS NULL OR e.dataEmprestimo <= :dataEmprestimoFim)
                  AND (:dataDevolucaoInicio IS NULL OR e.dataDevolucao >= :dataDevolucaoInicio)
                  AND (:dataDevolucaoFim IS NULL OR e.dataDevolucao <= :dataDevolucaoFim)
            """)
    Page<EmprestimoModel> buscarAvancado(
            @Param("statusEmprestimo") StatusEmprestimo statusEmprestimo,
            @Param("tombo") String tombo,
            @Param("livroNome") String livroNome,
            @Param("alunoNome") String alunoNome,
            @Param("dataEmprestimo") LocalDateTime dataEmprestimoInicio,
            @Param("dataDevolucao") LocalDateTime dataDevolucaoFim,
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

    @Query("""
                SELECT new br.com.lumilivre.api.data.ListaEmprestimoDTO(
                    e.statusEmprestimo,
                    l.nome,
                    ex.tombo,
                    a.nome,
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
    Page<ListaEmprestimoDTO> findEmprestimoParaListaAdmin(Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.data.ListaEmprestimoDashboardDTO(
                    livro.nome,
                    aluno.nome,
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
    List<ListaEmprestimoDashboardDTO> findEmprestimosAtivosEAtrasados();

}
