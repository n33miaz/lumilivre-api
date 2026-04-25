package br.com.lumilivre.api.dto.usuario;

import java.util.UUID;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private UUID id;
    private String email;
    private Role role;
    private String matriculaAluno;

    public UsuarioResponse(AppUser usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole();
        this.matriculaAluno = (usuario.getStudent() != null) ? usuario.getStudent().getRegistrationNumber() : null;
    }
}
