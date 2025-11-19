package br.com.lumilivre.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class MudarSenhaTokenRequest {
    @NotBlank
    private String token;
    @NotBlank
    private String novaSenha;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}