package br.com.lumilivre.api.dto.usuario;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.UsuarioModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Integer id;
    private String email;
    private Role role;
    private String matriculaAluno;

    public UsuarioResponse(UsuarioModel usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole();
        this.matriculaAluno = (usuario.getAluno() != null) ? usuario.getAluno().getMatricula() : null;
    }
}