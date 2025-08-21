package br.com.lumilivre.api.data;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.UsuarioModel;

public class LoginResponseDTO {

    private Integer id;
    private String email;
    private String role;
    private String matriculaAluno;

    public LoginResponseDTO(UsuarioModel usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole().name();
        if (usuario.getAluno() != null) {
            this.matriculaAluno = usuario.getAluno().getMatricula();
        }
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getMatriculaAluno() {
        return matriculaAluno;
    }
}
