package br.com.lumilivre.api.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.dto.reader.ReaderListItem;
import br.com.lumilivre.api.dto.reader.ReaderRankingItem;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.model.Reader;

public interface ReaderRepository extends JpaRepository<Reader, UUID> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByCpf(String cpf);

    Optional<Reader> findByRegistrationNumber(String registrationNumber);

    Optional<Reader> findByCpf(String cpf);

    Optional<Reader> findByFullNameIgnoreCase(String fullName);

    // Contagens globais por penalidade para os cartões da tela de leitores. O
    // "sem penalidade" é derivado (total - demais), então não precisa de método
    // próprio e ainda cobre quem tem REGISTRO, que a tela não exibe como cartão.
    long countByPenaltyCode(PenaltyCode penaltyCode);

    long countByPenaltyCodeIn(List<PenaltyCode> penaltyCodes);

    @Query("""
            SELECT a FROM Reader a
            WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR a.registrationNumber LIKE CONCAT('%', :texto, '%')
               OR LOWER(a.email) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR a.phoneNumber LIKE CONCAT('%', :texto, '%')
            """)
    Page<Reader> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.reader.ReaderListItem(
                a.penaltyCode,
                a.registrationNumber,
                c.name,
                a.readerCategory,
                a.fullName,
                a.birthDate,
                a.email,
                a.phoneNumber
            )
            FROM Reader a
            LEFT JOIN a.course c
            """)
    Page<ReaderListItem> findReaderListItems(Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.reader.ReaderListItem(
                a.penaltyCode, a.registrationNumber, c.name, a.readerCategory, a.fullName, a.birthDate, a.email, a.phoneNumber
            )
            FROM Reader a
            LEFT JOIN a.course c
            WHERE a.fullName ILIKE CONCAT('%', :texto, '%')
               OR a.registrationNumber LIKE CONCAT('%', :texto, '%')
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR a.phoneNumber LIKE CONCAT('%', :texto, '%')
               OR LOWER(a.email) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<ReaderListItem> findReaderListItemsByText(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT a.registrationNumber FROM Reader a")
    Set<String> findAllMatriculas();

    @Query("SELECT a.cpf FROM Reader a WHERE a.cpf IS NOT NULL")
    Set<String> findAllCpfs();

    @Query("""
            SELECT a FROM Reader a
            LEFT JOIN FETCH a.course
            LEFT JOIN FETCH a.studyShift
            LEFT JOIN FETCH a.academicModule
            WHERE (:idModulo IS NULL OR a.academicModule.id = :idModulo)
              AND (:idCurso IS NULL OR a.course.id = :idCurso)
              AND (:idTurno IS NULL OR a.studyShift.id = :idTurno)
              AND (:penalidade IS NULL OR a.penaltyCode = :penalidade)
              AND (cast(:inicio as timestamp) IS NULL OR a.createdAt >= :inicio)
              AND (cast(:fim as timestamp) IS NULL OR a.createdAt <= :fim)
            """)
    List<Reader> findForReport(
            @Param("idModulo") Integer idModulo,
            @Param("idCurso") Integer idCurso,
            @Param("idTurno") Integer idTurno,
            @Param("penalidade") PenaltyCode penalidade,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim);

    @Query("""
            SELECT new br.com.lumilivre.api.dto.reader.ReaderListItem(
                a.penaltyCode,
                a.registrationNumber,
                c.name,
                a.readerCategory,
                a.fullName,
                a.birthDate,
                a.email,
                a.phoneNumber
            )
            FROM Reader a
            LEFT JOIN a.course c
            LEFT JOIN a.studyShift t
            LEFT JOIN a.academicModule m
            WHERE (:penalidadeEnum IS NULL OR a.penaltyCode = :penalidadeEnum)
              AND (:matricula IS NULL OR a.registrationNumber = :matricula)
              AND (:nomeCompleto IS NULL OR a.fullName ILIKE :nomeCompleto)
              AND (:cursoNome IS NULL OR c.name ILIKE :cursoNome)
              AND (:turnoId IS NULL OR t.id = :turnoId)
              AND (:moduloId IS NULL OR m.id = :moduloId)
              AND (cast(:dataNascimento as date) IS NULL OR a.birthDate = :dataNascimento)
              AND (:email IS NULL OR a.email ILIKE :email)
              AND (:celular IS NULL OR a.phoneNumber = :celular)
            """)
    Page<ReaderListItem> buscarAvancadoV2(
            @Param("penalidadeEnum") PenaltyCode penalidadeEnum,
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
            SELECT new br.com.lumilivre.api.dto.reader.ReaderRankingItem(
                a.registrationNumber,
                a.fullName,
                COUNT(l)
            )
            FROM Reader a
            LEFT JOIN a.course c
            LEFT JOIN a.studyShift t
            LEFT JOIN a.academicModule m
            LEFT JOIN Loan l ON l.reader = a AND l.status IN (
                br.com.lumilivre.api.enums.LoanStatus.ACTIVE,
                br.com.lumilivre.api.enums.LoanStatus.COMPLETED,
                br.com.lumilivre.api.enums.LoanStatus.OVERDUE
            )
            WHERE (:cursoId IS NULL OR c.id = :cursoId)
              AND (:moduloId IS NULL OR m.id = :moduloId)
              AND (:turnoId IS NULL OR t.id = :turnoId)
            GROUP BY a.registrationNumber, a.fullName
            ORDER BY COUNT(l) DESC
            """)
    Page<ReaderRankingItem> findRankingItems(
            @Param("cursoId") Integer cursoId,
            @Param("moduloId") Integer moduloId,
            @Param("turnoId") Integer turnoId,
            Pageable pageable);
}
