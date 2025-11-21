package br.com.lumilivre.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "user não deve estar em branco")
    private String user;

    @NotBlank(message = "senha não deve estar em branco")
    private String senha;
}