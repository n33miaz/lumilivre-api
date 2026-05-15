package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoAtivoResponse;
import br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoDashboardResponse;
import br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoListagemResponse;
import br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoResponse;
import br.com.lumilivre.api.dto.loan.ActiveLoanItem;
import br.com.lumilivre.api.dto.loan.LoanListItem;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.model.Loan;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByStatusIn(List<LoanStatus> statuses);

    long countByStatusIn(List<LoanStatus> statuses);

    List<Loan> findByStatusAndDueAtBefore(LoanStatus status, OffsetDateTime now);

    List<Loan> findByStatusAndDueAtGreaterThanEqual(LoanStatus status, OffsetDateTime now);

    List<Loan> findByStudent_RegistrationNumber(String registrationNumber);

    List<Loan> findByBorrowedAtGreaterThanEqual(OffsetDateTime dataInicio);

    List<Loan> findByBookCopy_CopyCode(String copyCode);

    List<Loan> findByBorrowedAtBetween(OffsetDateTime inicio, OffsetDateTime fim);

    List<Loan> findByDueAtBetween(OffsetDateTime inicio, OffsetDateTime fim);

    boolean existsByBookCopy_CopyCodeAndStatus(String copyCode, LoanStatus status);

    boolean existsByBookCopy_CopyCodeAndStatusIn(String copyCode, List<LoanStatus> statuses);

    long countByStudent_RegistrationNumberAndStatus(String registrationNumber, LoanStatus status);

    List<Loan> findByStatus(LoanStatus status);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoListagemResponse(
                e.id,
                e.status,
                l.title,
                ex.copyCode,
                a.fullName,
                a.registrationNumber,
                c.name,
                e.borrowedAt,
                e.dueAt
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            JOIN a.course c
            WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR a.registrationNumber LIKE CONCAT('%', :texto, '%')
               OR LOWER(l.title) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR ex.copyCode LIKE CONCAT('%', :texto, '%')
            """)
    Page<EmprestimoListagemResponse> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.loan.LoanListItem(
                e.id,
                e.status,
                l.title,
                ex.copyCode,
                a.fullName,
                a.registrationNumber,
                c.name,
                e.borrowedAt,
                e.dueAt
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            JOIN a.course c
            WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR a.registrationNumber LIKE CONCAT('%', :texto, '%')
               OR LOWER(l.title) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR ex.copyCode LIKE CONCAT('%', :texto, '%')
            """)
    Page<LoanListItem> searchListItems(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoListagemResponse(
                e.id,
                e.status,
                l.title,
                ex.copyCode,
                a.fullName,
                a.registrationNumber,
                c.name,
                e.borrowedAt,
                e.dueAt
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            JOIN a.course c
            WHERE
            (
                (:statusEmprestimo IS NULL) OR
                (:statusEmprestimo = 'COMPLETED' AND e.status = br.com.lumilivre.api.enums.LoanStatus.COMPLETED) OR
                (:statusEmprestimo = 'OVERDUE' AND (e.status = br.com.lumilivre.api.enums.LoanStatus.OVERDUE OR (e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE AND e.dueAt < :now))) OR
                (:statusEmprestimo = 'ACTIVE' AND (e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE AND e.dueAt >= :now))
            )
            AND (:tombo IS NULL OR ex.copyCode ILIKE :tombo)
            AND (:livroNome IS NULL OR l.title ILIKE :livroNome)
            AND (:alunoNomeCompleto IS NULL OR a.fullName ILIKE :alunoNomeCompleto)
            AND (cast(:dataEmprestimoInicio as timestamp) IS NULL OR e.borrowedAt >= :dataEmprestimoInicio)
            AND (cast(:dataEmprestimoFim as timestamp) IS NULL OR e.borrowedAt <= :dataEmprestimoFim)
            AND (cast(:dataDevolucaoInicio as timestamp) IS NULL OR e.dueAt >= :dataDevolucaoInicio)
            AND (cast(:dataDevolucaoFim as timestamp) IS NULL OR e.dueAt <= :dataDevolucaoFim)
            """)
    Page<EmprestimoListagemResponse> buscarAvancado(
            @Param("statusEmprestimo") String statusEmprestimo,
            @Param("tombo") String tombo,
            @Param("livroNome") String livroNome,
            @Param("alunoNomeCompleto") String alunoNomeCompleto,
            @Param("dataEmprestimoInicio") OffsetDateTime dataEmprestimoInicio,
            @Param("dataEmprestimoFim") OffsetDateTime dataEmprestimoFim,
            @Param("dataDevolucaoInicio") OffsetDateTime dataDevolucaoInicio,
            @Param("dataDevolucaoFim") OffsetDateTime dataDevolucaoFim,
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.loan.LoanListItem(
                e.id,
                e.status,
                l.title,
                ex.copyCode,
                a.fullName,
                a.registrationNumber,
                c.name,
                e.borrowedAt,
                e.dueAt
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            JOIN a.course c
            WHERE
            (
                (:statusEmprestimo IS NULL) OR
                (:statusEmprestimo = 'COMPLETED' AND e.status = br.com.lumilivre.api.enums.LoanStatus.COMPLETED) OR
                (:statusEmprestimo = 'OVERDUE' AND (e.status = br.com.lumilivre.api.enums.LoanStatus.OVERDUE OR (e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE AND e.dueAt < :now))) OR
                (:statusEmprestimo = 'ACTIVE' AND (e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE AND e.dueAt >= :now))
            )
            AND (:tombo IS NULL OR ex.copyCode ILIKE :tombo)
            AND (:livroNome IS NULL OR l.title ILIKE :livroNome)
            AND (:alunoNomeCompleto IS NULL OR a.fullName ILIKE :alunoNomeCompleto)
            AND (cast(:dataEmprestimoInicio as timestamp) IS NULL OR e.borrowedAt >= :dataEmprestimoInicio)
            AND (cast(:dataEmprestimoFim as timestamp) IS NULL OR e.borrowedAt <= :dataEmprestimoFim)
            AND (cast(:dataDevolucaoInicio as timestamp) IS NULL OR e.dueAt >= :dataDevolucaoInicio)
            AND (cast(:dataDevolucaoFim as timestamp) IS NULL OR e.dueAt <= :dataDevolucaoFim)
            """)
    Page<LoanListItem> searchAdvancedListItems(
            @Param("statusEmprestimo") String statusEmprestimo,
            @Param("tombo") String tombo,
            @Param("livroNome") String livroNome,
            @Param("alunoNomeCompleto") String alunoNomeCompleto,
            @Param("dataEmprestimoInicio") OffsetDateTime dataEmprestimoInicio,
            @Param("dataEmprestimoFim") OffsetDateTime dataEmprestimoFim,
            @Param("dataDevolucaoInicio") OffsetDateTime dataDevolucaoInicio,
            @Param("dataDevolucaoFim") OffsetDateTime dataDevolucaoFim,
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoResponse(
                e.id,
                e.borrowedAt,
                e.dueAt,
                e.status,
                e.penaltyCode,
                e.bookCopy.book.id,
                e.bookCopy.book.title,
                e.bookCopy.book.coverUrl
            )
            FROM Loan e
            WHERE e.student.registrationNumber = :matricula
              AND e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE
            """)
    List<EmprestimoResponse> findEmprestimosAtivos(@Param("matricula") String matricula);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoResponse(
                e.id,
                e.borrowedAt,
                e.dueAt,
                e.status,
                e.penaltyCode,
                e.bookCopy.book.id,
                e.bookCopy.book.title,
                e.bookCopy.book.coverUrl
            )
            FROM Loan e
            WHERE e.student.registrationNumber = :matricula
              AND e.status = br.com.lumilivre.api.enums.LoanStatus.COMPLETED
            """)
    List<EmprestimoResponse> findHistoricoEmprestimos(@Param("matricula") String matricula);

    @Query("""
            SELECT e
            FROM Loan e
            JOIN FETCH e.bookCopy ex
            JOIN FETCH ex.book
            JOIN FETCH e.student a
            LEFT JOIN FETCH a.course
            WHERE a.registrationNumber = :matricula
              AND e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE
            """)
    List<Loan> findActiveLoansForStudent(@Param("matricula") String matricula);

    @Query("""
            SELECT e
            FROM Loan e
            JOIN FETCH e.bookCopy ex
            JOIN FETCH ex.book
            JOIN FETCH e.student a
            LEFT JOIN FETCH a.course
            WHERE a.registrationNumber = :matricula
              AND e.status = br.com.lumilivre.api.enums.LoanStatus.COMPLETED
            """)
    List<Loan> findLoanHistoryForStudent(@Param("matricula") String matricula);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoListagemResponse(
                e.id,
                e.status,
                l.title,
                ex.copyCode,
                a.fullName,
                a.registrationNumber,
                a.course.name,
                e.borrowedAt,
                e.dueAt
            )
            FROM Loan e
            JOIN e.bookCopy ex
            JOIN ex.book l
            JOIN e.student a
            """)
    Page<EmprestimoListagemResponse> findEmprestimoParaListaAdmin(Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.loan.LoanListItem(
                e.id,
                e.status,
                l.title,
                ex.copyCode,
                a.fullName,
                a.registrationNumber,
                a.course.name,
                e.borrowedAt,
                e.dueAt
            )
            FROM Loan e
            JOIN e.bookCopy ex
            JOIN ex.book l
            JOIN e.student a
            """)
    Page<LoanListItem> findLoanListItems(Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoDashboardResponse(
                livro.title,
                aluno.fullName,
                emprestimo.dueAt,
                emprestimo.status
            )
            FROM Loan emprestimo
            JOIN emprestimo.bookCopy exemplar
            JOIN exemplar.book livro
            JOIN emprestimo.student aluno
            WHERE emprestimo.status IN (
                br.com.lumilivre.api.enums.LoanStatus.ACTIVE,
                br.com.lumilivre.api.enums.LoanStatus.OVERDUE
            )
            ORDER BY emprestimo.dueAt ASC
            """)
    List<EmprestimoDashboardResponse> findEmprestimosAtivosEAtrasados();

    @Query("""
            SELECT DISTINCT e FROM Loan e
            LEFT JOIN FETCH e.student a
            LEFT JOIN FETCH a.course
            LEFT JOIN FETCH a.academicModule
            LEFT JOIN FETCH e.bookCopy ex
            LEFT JOIN FETCH ex.book l
            WHERE (cast(:inicio as timestamp) IS NULL OR e.borrowedAt >= :inicio)
              AND (cast(:fim as timestamp) IS NULL OR e.borrowedAt <= :fim)
              AND (:status IS NULL OR e.status = :status)
              AND (:matriculaAluno IS NULL OR a.registrationNumber ILIKE :matriculaAluno OR a.fullName ILIKE :matriculaAluno)
              AND (cast(:idCurso as integer) IS NULL OR a.course.id = :idCurso)
              AND (cast(:idModulo as integer) IS NULL OR a.academicModule.id = :idModulo)
              AND (
                    :isbnOuTombo IS NULL
                    OR ex.copyCode ILIKE :isbnOuTombo
                    OR l.isbn ILIKE :isbnOuTombo
                    OR l.title ILIKE :isbnOuTombo
              )
            ORDER BY e.borrowedAt DESC
            """)
    List<Loan> findForReport(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("status") LoanStatus status,
            @Param("matriculaAluno") String matriculaAluno,
            @Param("idCurso") Integer idCurso,
            @Param("isbnOuTombo") String isbnOuTombo,
            @Param("idModulo") Integer idModulo);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoAtivoResponse(
                e.id,
                l.title,
                a.fullName,
                a.registrationNumber,
                ex.copyCode,
                CAST(e.borrowedAt AS LocalDate),
                CAST(e.dueAt AS LocalDate),
                e.status
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            WHERE e.status IN (br.com.lumilivre.api.enums.LoanStatus.ACTIVE, br.com.lumilivre.api.enums.LoanStatus.OVERDUE)
            ORDER BY e.dueAt ASC
            """)
    List<EmprestimoAtivoResponse> findAtivosEAtrasadosDTO();

    @Query("""
            SELECT new br.com.lumilivre.api.dto.loan.ActiveLoanItem(
                e.id,
                l.title,
                a.fullName,
                a.registrationNumber,
                ex.copyCode,
                CAST(e.borrowedAt AS LocalDate),
                CAST(e.dueAt AS LocalDate),
                e.status
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            WHERE e.status IN (br.com.lumilivre.api.enums.LoanStatus.ACTIVE, br.com.lumilivre.api.enums.LoanStatus.OVERDUE)
            ORDER BY e.dueAt ASC
            """)
    List<ActiveLoanItem> findActiveAndOverdueItems();

    @Query("""
            SELECT new br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoAtivoResponse(
                e.id,
                l.title,
                a.fullName,
                a.registrationNumber,
                ex.copyCode,
                CAST(e.borrowedAt AS LocalDate),
                CAST(e.dueAt AS LocalDate),
                e.status
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            WHERE e.status = br.com.lumilivre.api.enums.LoanStatus.OVERDUE
               OR (e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE AND e.dueAt < :dataRef)
            ORDER BY e.dueAt ASC
            """)
    List<EmprestimoAtivoResponse> findApenasAtrasadosDTO(@Param("dataRef") OffsetDateTime dataRef);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.loan.ActiveLoanItem(
                e.id,
                l.title,
                a.fullName,
                a.registrationNumber,
                ex.copyCode,
                CAST(e.borrowedAt AS LocalDate),
                CAST(e.dueAt AS LocalDate),
                e.status
            )
            FROM Loan e
            JOIN e.student a
            JOIN e.bookCopy ex
            JOIN ex.book l
            WHERE e.status = br.com.lumilivre.api.enums.LoanStatus.OVERDUE
               OR (e.status = br.com.lumilivre.api.enums.LoanStatus.ACTIVE AND e.dueAt < :dataRef)
            ORDER BY e.dueAt ASC
            """)
    List<ActiveLoanItem> findOverdueItems(@Param("dataRef") OffsetDateTime dataRef);

    default Double avgReturnDays() {
        return findByStatus(LoanStatus.COMPLETED).stream()
                .filter(loan -> loan.getBorrowedAt() != null && loan.getDueAt() != null)
                .mapToDouble(loan -> java.time.Duration
                        .between(loan.getBorrowedAt(), loan.getDueAt())
                        .toHours() / 24.0)
                .average()
                .orElse(0.0);
    }
}
