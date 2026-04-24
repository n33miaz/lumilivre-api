package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.dto.aluno.AlunoRankingResponse;
import br.com.lumilivre.api.dto.aluno.AlunoResumoResponse;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.model.Student;

public interface StudentRepository extends JpaRepository<Student, String> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByCpf(String cpf);

    Optional<Student> findByRegistrationNumber(String registrationNumber);

    Optional<Student> findByCpf(String cpf);

    Optional<Student> findByFullNameIgnoreCase(String fullName);

    List<Student> findAllByOrderByLoansCountDesc();

    default boolean existsByMatricula(String matricula) {
        return existsByRegistrationNumber(matricula);
    }

    default Optional<Student> findByMatricula(String matricula) {
        return findByRegistrationNumber(matricula);
    }

    default Optional<Student> findByNomeCompletoIgnoreCase(String nomeCompleto) {
        return findByFullNameIgnoreCase(nomeCompleto);
    }

    default List<Student> findAllByOrderByEmprestimosCountDesc() {
        return findAllByOrderByLoansCountDesc();
    }

    @Query(value = """
                SELECT * FROM aluno a
                WHERE a.texto_busca @@ plainto_tsquery('portuguese', :texto)
            """, countQuery = """
                SELECT count(*) FROM aluno a
                WHERE a.texto_busca @@ plainto_tsquery('portuguese', :texto)
            """, nativeQuery = true)
    Page<Student> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query(value = """
                SELECT a FROM Student a
                JOIN FETCH a.course c
                LEFT JOIN FETCH a.studyShift
                LEFT JOIN FETCH a.academicModule
                WHERE (:penalidadeEnum IS NULL OR a.penaltyCode = :penalidadeEnum)
                  AND (:matricula IS NULL OR a.registrationNumber = :matricula)
                  AND (:nomeCompleto IS NULL OR a.fullName ILIKE :nomeCompleto)
                  AND (:cursoNome IS NULL OR c.name ILIKE :cursoNome)
                  AND (:turnoId IS NULL OR a.studyShift.id = :turnoId)
                  AND (:moduloId IS NULL OR a.academicModule.id = :moduloId)
                  AND (:dataNascimento IS NULL OR a.birthDate = :dataNascimento)
                  AND (:email IS NULL OR a.email ILIKE :email)
                  AND (:celular IS NULL OR a.phoneNumber = :celular)
            """, countQuery = """
                SELECT COUNT(a) FROM Student a
                LEFT JOIN a.course c
                WHERE (:penalidadeEnum IS NULL OR a.penaltyCode = :penalidadeEnum)
                  AND (:matricula IS NULL OR a.registrationNumber = :matricula)
                  AND (:nomeCompleto IS NULL OR a.fullName ILIKE :nomeCompleto)
                  AND (:cursoNome IS NULL OR c.name ILIKE :cursoNome)
                  AND (:turnoId IS NULL OR a.studyShift.id = :turnoId)
                  AND (:moduloId IS NULL OR a.academicModule.id = :moduloId)
                  AND (:dataNascimento IS NULL OR a.birthDate = :dataNascimento)
                  AND (:email IS NULL OR a.email ILIKE :email)
                  AND (:celular IS NULL OR a.phoneNumber = :celular)
            """)
    Page<Student> buscarAvancado(
            @Param("penalidadeEnum") Penalidade penalidadeEnum,
            @Param("matricula") String matricula,
            @Param("nomeCompleto") String nomeCompleto,
            @Param("cursoNome") String cursoNome,
            @Param("turnoId") Integer turnoId,
            @Param("moduloId") Integer moduloId,
            @Param("dataNascimento") LocalDate dataNascimento,
            @Param("email") String email,
            @Param("celular") String celular,
            Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.aluno.AlunoResumoResponse(
                    a.penaltyCode,
                    a.registrationNumber,
                    c.name,
                    a.fullName,
                    a.birthDate,
                    a.email,
                    a.phoneNumber
                )
                FROM Student a
                JOIN a.course c
            """)
    Page<AlunoResumoResponse> findAlunosParaListaAdmin(Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.aluno.AlunoResumoResponse(
                    a.penaltyCode, a.registrationNumber, c.name, a.fullName, a.birthDate, a.email, a.phoneNumber
                )
                FROM Student a
                JOIN a.course c
                WHERE a.fullName ILIKE CONCAT('%', :texto, '%')
                   OR a.registrationNumber LIKE CONCAT('%', :texto, '%')
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR a.phoneNumber LIKE CONCAT('%', :texto, '%')
                   OR LOWER(a.email) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<AlunoResumoResponse> findAlunosParaListaAdminComFiltro(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT a.registrationNumber FROM Student a")
    Set<String> findAllMatriculas();

    @Query("SELECT a.cpf FROM Student a WHERE a.cpf IS NOT NULL")
    Set<String> findAllCpfs();

    @Query("""
                SELECT a FROM Student a
                JOIN FETCH a.course
                LEFT JOIN FETCH a.studyShift
                LEFT JOIN FETCH a.academicModule
                WHERE (:idModulo IS NULL OR a.academicModule.id = :idModulo)
                  AND (:idCurso IS NULL OR a.course.id = :idCurso)
                  AND (:idTurno IS NULL OR a.studyShift.id = :idTurno)
                  AND (:penalidade IS NULL OR a.penaltyCode = :penalidade)
                  AND (cast(:inicio as date) IS NULL OR a.createdAt >= :inicio)
                  AND (cast(:fim as date) IS NULL OR a.createdAt <= :fim)
            """)
    List<Student> findForReport(
            @Param("idModulo") Integer idModulo,
            @Param("idCurso") Integer idCurso,
            @Param("idTurno") Integer idTurno,
            @Param("penalidade") Penalidade penalidade,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.aluno.AlunoResumoResponse(
                a.penaltyCode,
                a.registrationNumber,
                c.name,
                a.fullName,
                a.birthDate,
                a.email,
                a.phoneNumber
            )
            FROM Student a
            JOIN a.course c
            LEFT JOIN a.studyShift t
            LEFT JOIN a.academicModule m
            WHERE (:penalidadeEnum IS NULL OR a.penaltyCode = :penalidadeEnum)
              AND (:matricula IS NULL OR a.registrationNumber = :matricula)
              AND (:nomeCompleto IS NULL OR a.fullName ILIKE :nomeCompleto)
              AND (:cursoNome IS NULL OR c.name ILIKE :cursoNome)
              AND (:turnoId IS NULL OR t.id = :turnoId)
              AND (:moduloId IS NULL OR m.id = :moduloId)
              AND (:dataNascimento IS NULL OR a.birthDate = :dataNascimento)
              AND (:email IS NULL OR a.email ILIKE :email)
              AND (:celular IS NULL OR a.phoneNumber = :celular)
            """)
    Page<AlunoResumoResponse> buscarAvancadoComDTO(
            @Param("penalidadeEnum") Penalidade penalidadeEnum,
            @Param("matricula") String matricula,
            @Param("nomeCompleto") String nomeCompleto,
            @Param("cursoNome") String cursoNome,
            @Param("turnoId") Integer turnoId,
            @Param("moduloId") Integer moduloId,
            @Param("dataNascimento") LocalDate dataNascimento,
            @Param("email") String email,
            @Param("celular") String celular,
            Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.aluno.AlunoRankingResponse(
                    a.registrationNumber,
                    a.fullName,
                    a.loansCount
                )
                FROM Student a
                LEFT JOIN a.course c
                LEFT JOIN a.studyShift t
                LEFT JOIN a.academicModule m
                WHERE (:cursoId IS NULL OR c.id = :cursoId)
                  AND (:moduloId IS NULL OR m.id = :moduloId)
                  AND (:turnoId IS NULL OR t.id = :turnoId)
                ORDER BY a.loansCount DESC
            """)
    Page<AlunoRankingResponse> findRankingComFiltros(
            @Param("cursoId") Integer cursoId,
            @Param("moduloId") Integer moduloId,
            @Param("turnoId") Integer turnoId,
            Pageable pageable);
}
