package br.com.lumilivre.api.dto.responses;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.UsuarioModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioResponseDTO {

    private Integer id;
    private String email;
    private Role role;
    private String matriculaAluno;

    public UsuarioResponseDTO() {
    }

    public UsuarioResponseDTO(UsuarioModel usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole();

        if (usuario.getAluno() != null) {
            this.matriculaAluno = usuario.getAluno().getMatricula();
        }
    }
}