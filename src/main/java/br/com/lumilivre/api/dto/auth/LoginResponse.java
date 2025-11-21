package br.com.lumilivre.api.dto.auth;

import br.com.lumilivre.api.model.UsuarioModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Integer id;
    private String email;
    private String role;
    private String matriculaAluno;
    private String token;

    public LoginResponse(UsuarioModel usuario, String token) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole().name();
        this.token = token;

        if (usuario.getAluno() != null) {
            this.matriculaAluno = usuario.getAluno().getMatricula();
        }
    }
}