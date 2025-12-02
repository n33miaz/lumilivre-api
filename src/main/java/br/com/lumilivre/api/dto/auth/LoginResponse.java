package br.com.lumilivre.api.dto.auth;

import br.com.lumilivre.api.model.UsuarioModel;
import com.fasterxml.jackson.annotation.JsonProperty; // <--- IMPORTANTE
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

    @JsonProperty("isInitialPassword")
    private boolean isInitialPassword;

    public LoginResponse(UsuarioModel usuario, String token, boolean isInitialPassword) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole().name();
        this.token = token;
        this.isInitialPassword = isInitialPassword;

        if (usuario.getAluno() != null) {
            this.matriculaAluno = usuario.getAluno().getMatricula();
        }
    }
}