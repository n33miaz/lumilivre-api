package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.dto.aluno.AlunoResumoResponse;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.model.AlunoModel;

public interface AlunoRepository extends JpaRepository<AlunoModel, String> {

    boolean existsByMatricula(String matricula);

    boolean existsByCpf(String cpf);

    Optional<AlunoModel> findByMatricula(String matricula);

    Optional<AlunoModel> findByCpf(String cpf);

    Optional<AlunoModel> findByNomeCompletoIgnoreCase(String nomeCompleto);

    List<AlunoModel> findAllByOrderByEmprestimosCountDesc();

    @Query(value = """
                SELECT *
                FROM aluno a
                WHERE a.texto_busca @@ plainto_tsquery('portuguese', :texto)
            """, nativeQuery = true)
    Page<AlunoModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query(value = """
                SELECT a FROM AlunoModel a
                JOIN FETCH a.curso c
                LEFT JOIN FETCH a.turno
                LEFT JOIN FETCH a.modulo
                WHERE (:penalidadeEnum IS NULL OR a.penalidade = :penalidadeEnum)
                  AND (:matricula IS NULL OR a.matricula = :matricula)
                  AND (:nomeCompleto IS NULL OR a.nomeCompleto ILIKE :nomeCompleto)
                  AND (:cursoNome IS NULL OR c.nome ILIKE :cursoNome)
                  AND (:turnoId IS NULL OR a.turno.id = :turnoId)
                  AND (:moduloId IS NULL OR a.modulo.id = :moduloId)
                  AND (:dataNascimento IS NULL OR a.dataNascimento = :dataNascimento)
                  AND (:email IS NULL OR a.email ILIKE :email)
                  AND (:celular IS NULL OR a.celular = :celular)
            """, countQuery = """
                SELECT COUNT(a) FROM AlunoModel a
                LEFT JOIN a.curso c
                WHERE (:penalidadeEnum IS NULL OR a.penalidade = :penalidadeEnum)
                  AND (:matricula IS NULL OR a.matricula = :matricula)
                  AND (:nomeCompleto IS NULL OR a.nomeCompleto ILIKE :nomeCompleto)
                  AND (:cursoNome IS NULL OR c.nome ILIKE :cursoNome)
                  AND (:turnoId IS NULL OR a.turno.id = :turnoId)
                  AND (:moduloId IS NULL OR a.modulo.id = :moduloId)
                  AND (:dataNascimento IS NULL OR a.dataNascimento = :dataNascimento)
                  AND (:email IS NULL OR a.email ILIKE :email)
                  AND (:celular IS NULL OR a.celular = :celular)
            """)
    Page<AlunoModel> buscarAvancado(
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
                    a.penalidade,
                    a.matricula,
                    c.nome,
                    a.nomeCompleto,
                    a.dataNascimento,
                    a.email,
                    a.celular
                )
                FROM AlunoModel a
                JOIN a.curso c
                ORDER BY a.nomeCompleto
            """)
    Page<AlunoResumoResponse> findAlunosParaListaAdmin(Pageable pageable);

    @Query("""
                SELECT new br.com.lumilivre.api.dto.aluno.AlunoResumoResponse(
                    a.penalidade, a.matricula, c.nome, a.nomeCompleto, a.dataNascimento, a.email, a.celular
                )
                FROM AlunoModel a
                JOIN a.curso c
                WHERE a.nomeCompleto ILIKE CONCAT('%', :texto, '%')
                   OR a.matricula LIKE CONCAT('%', :texto, '%')
                   OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR a.celular LIKE CONCAT('%', :texto, '%')
                   OR LOWER(a.email) LIKE LOWER(CONCAT('%', :texto, '%'))
                ORDER BY a.nomeCompleto
            """)
    Page<AlunoResumoResponse> findAlunosParaListaAdminComFiltro(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT a.matricula FROM AlunoModel a")
    Set<String> findAllMatriculas();

    @Query("SELECT a.cpf FROM AlunoModel a WHERE a.cpf IS NOT NULL")
    Set<String> findAllCpfs();

    @Query("""
                SELECT a FROM AlunoModel a
                JOIN FETCH a.curso
                LEFT JOIN FETCH a.turno
                LEFT JOIN FETCH a.modulo
                WHERE (:idModulo IS NULL OR a.modulo.id = :idModulo)
                  AND (:idCurso IS NULL OR a.curso.id = :idCurso)
                  AND (:idTurno IS NULL OR a.turno.id = :idTurno)
                  AND (:penalidade IS NULL OR a.penalidade = :penalidade)
            """)
    List<AlunoModel> findForReport(
            @Param("idModulo") Integer idModulo,
            @Param("idCurso") Integer idCurso,
            @Param("idTurno") Integer idTurno,
            @Param("penalidade") Penalidade penalidade);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.aluno.AlunoResumoResponse(
                a.penalidade,
                a.matricula,
                c.nome,
                a.nomeCompleto,
                a.dataNascimento,
                a.email,
                a.celular
            )
            FROM AlunoModel a
            JOIN a.curso c
            LEFT JOIN a.turno t
            LEFT JOIN a.modulo m
            WHERE (:penalidadeEnum IS NULL OR a.penalidade = :penalidadeEnum)
              AND (:matricula IS NULL OR a.matricula = :matricula)
              AND (:nomeCompleto IS NULL OR a.nomeCompleto ILIKE :nomeCompleto)
              AND (:cursoNome IS NULL OR c.nome ILIKE :cursoNome)
              AND (:turnoId IS NULL OR t.id = :turnoId)
              AND (:moduloId IS NULL OR m.id = :moduloId)
              AND (:dataNascimento IS NULL OR a.dataNascimento = :dataNascimento)
              AND (:email IS NULL OR a.email ILIKE :email)
              AND (:celular IS NULL OR a.celular = :celular)
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
}