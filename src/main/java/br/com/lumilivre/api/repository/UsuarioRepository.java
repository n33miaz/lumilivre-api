package br.com.lumilivre.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.data.ListaUsuarioDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.UsuarioModel;

public interface UsuarioRepository extends JpaRepository<UsuarioModel, Integer> {

    boolean existsByEmail(String email);

    boolean existsByAluno(AlunoModel aluno);

    Optional<UsuarioModel> findByEmail(String email);

    List<UsuarioModel> findByRole(Role role);

    Optional<UsuarioModel> findByEmailOrAluno_Matricula(String email, String matricula);

    @Query("""
                SELECT u FROM UsuarioModel u
                WHERE CAST(u.id AS string) LIKE CONCAT('%', :texto, '%')
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR LOWER(u.role) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<UsuarioModel> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
                SELECT u FROM UsuarioModel u
                WHERE (:id IS NULL OR CAST(u.id AS string) LIKE CONCAT('%', :id, '%'))
                  AND (:email IS NULL OR u.email = :email)
                  AND (:role IS NULL OR LOWER(u.role) LIKE LOWER(CONCAT('%', :role, '%')))
            """)
    Page<UsuarioModel> buscarAvancado(
            @Param("id") Integer id,
            @Param("email") String email,
            @Param("role") Role role,
            Pageable pageable);

    @Query("""
            SELECT new br.com.lumilivre.api.data.ListaUsuarioDTO(
                u.id,
                u.email,
                u.role
            )
            FROM UsuarioModel u
            ORDER BY u.id
            """)
    Page<ListaUsuarioDTO> findUsuarioParaListaAdmin(Pageable pageable);

}
