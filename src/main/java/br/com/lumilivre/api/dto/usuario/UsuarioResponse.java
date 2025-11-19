package br.com.lumilivre.api.dto.usuario;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.UsuarioModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioResponse {

    private Integer id;
    private String email;
    private Role role;
    private String matriculaAluno;

    public UsuarioResponse() {
    }

    public UsuarioResponse(UsuarioModel usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole();

        if (usuario.getAluno() != null) {
            this.matriculaAluno = usuario.getAluno().getMatricula();
        }
    }
}