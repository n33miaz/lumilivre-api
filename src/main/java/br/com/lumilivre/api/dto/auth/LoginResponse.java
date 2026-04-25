package br.com.lumilivre.api.dto.auth;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.lumilivre.api.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UUID id;
    private String email;
    private String role;
    private String matriculaAluno;
    private String token;

    @JsonProperty("isInitialPassword")
    private boolean isInitialPassword;

    public LoginResponse(AppUser usuario, String token, boolean isInitialPassword) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.role = usuario.getRole().name();
        this.token = token;
        this.isInitialPassword = isInitialPassword;

        if (usuario.getStudent() != null) {
            this.matriculaAluno = usuario.getStudent().getRegistrationNumber();
        }
    }
}
