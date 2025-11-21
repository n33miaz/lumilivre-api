package br.com.lumilivre.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MudarSenhaTokenRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String novaSenha;
}