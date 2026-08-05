package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Reader;

// A busca avancada da aba Usuarios usa Specification (montada no AppUserService).
// A versao em JPQL nao funcionava no Postgres: comparava o enum com LIKE
// (LOWER(u.role) virava lower(bytea) e estourava 42883) e deixava o UUID solto
// num ":id IS NULL", sem tipo para o Postgres inferir. Com Specification, filtro
// nulo nao gera predicado nenhum — nao ha parametro sem tipo para inferir.
public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {

    boolean existsByEmail(String email);

    boolean existsByReader(Reader reader);

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByRole(Role role);

    Optional<AppUser> findByEmailOrReader_RegistrationNumber(String email, String registrationNumber);

    default Optional<AppUser> findByEmailOrRegistrationNumber(String email, String matricula) {
        return findByEmailOrReader_RegistrationNumber(email, matricula);
    }

    /**
     * Busca de autenticação: conta com {@code deleted_at} preenchido não existe
     * mais para efeito de login. LEFT JOIN porque conta de staff não tem leitor.
     */
    @Query("""
                        SELECT u FROM AppUser u
                        LEFT JOIN u.reader r
                        WHERE u.deletedAt IS NULL
                          AND (u.email = :login OR r.registrationNumber = :login)
                    """)
    Optional<AppUser> findAliveByLogin(@Param("login") String login);

    /**
     * Quantos ADMIN ainda podem entrar. Base da regra que impede tijolar o
     * sistema desativando/bloqueando o último administrador.
     */
    @Query("""
                        SELECT COUNT(u) FROM AppUser u
                        WHERE u.role = br.com.lumilivre.api.enums.Role.ADMIN
                          AND u.active = TRUE
                          AND u.locked = FALSE
                          AND u.deletedAt IS NULL
                    """)
    long countUsableAdmins();

    @Query("""
                        SELECT u FROM AppUser u
                        WHERE CAST(u.id AS string) LIKE CONCAT('%', :texto, '%')
                           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :texto, '%'))
                           OR LOWER(CAST(u.role AS string)) LIKE LOWER(CONCAT('%', :texto, '%'))
                    """)
    Page<AppUser> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

}
