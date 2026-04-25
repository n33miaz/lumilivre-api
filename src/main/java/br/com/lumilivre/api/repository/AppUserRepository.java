package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Student;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    boolean existsByEmail(String email);

    boolean existsByStudent(Student student);

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByRole(Role role);

    Optional<AppUser> findByEmailOrStudent_RegistrationNumber(String email, String registrationNumber);

    default Optional<AppUser> findByEmailOrRegistrationNumber(String email, String matricula) {
        return findByEmailOrStudent_RegistrationNumber(email, matricula);
    }

    @Query("""
                        SELECT u FROM AppUser u
                        WHERE CAST(u.id AS string) LIKE CONCAT('%', :texto, '%')
                           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :texto, '%'))
                           OR LOWER(CAST(u.role AS string)) LIKE LOWER(CONCAT('%', :texto, '%'))
                    """)
    Page<AppUser> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                        SELECT u FROM AppUser u
                        WHERE (:id IS NULL OR u.id = :id)
                          AND (:email IS NULL OR u.email = :email)
                          AND (:role IS NULL OR LOWER(u.role) LIKE LOWER(CONCAT('%', :role, '%')))
                    """)
    Page<AppUser> buscarAvancado(
            @Param("id") UUID id,
            @Param("email") String email,
            @Param("role") Role role,
            Pageable pageable);

    @Query("""
                    SELECT new br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse(
                        u.id,
                        u.email,
                        u.role
                    )
                    FROM AppUser u
                    ORDER BY u.id
                    """)
    Page<UsuarioResumoResponse> findUsuarioParaListaAdmin(Pageable pageable);

    @Query("""
                        SELECT new br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse(u.id, u.email, u.role)
                        FROM AppUser u
                        WHERE (:texto IS NULL OR :texto = ''
                           OR CAST(u.id AS string) LIKE %:texto%
                           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :texto, '%'))
                           OR LOWER(CAST(u.role AS string)) LIKE LOWER(CONCAT('%', :texto, '%')))
                    """)
    Page<UsuarioResumoResponse> buscarPorTextoComDTO(@Param("texto") String texto, Pageable pageable);

    @Query("""
                        SELECT new br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse(u.id, u.email, u.role)
                        FROM AppUser u
                        WHERE (:id IS NULL OR u.id = :id)
                          AND (:email IS NULL OR u.email ILIKE CONCAT('%', :email, '%'))
                          AND (:role IS NULL OR u.role = :role)
                    """)
    Page<UsuarioResumoResponse> buscarAvancadoComDTO(
            @Param("id") UUID id,
            @Param("email") String email,
            @Param("role") Role role,
            Pageable pageable);
}
