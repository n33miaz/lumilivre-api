package br.com.lumilivre.api.dto.auth;

import br.com.lumilivre.api.model.UsuarioModel;

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
        if (usuario.getAluno() != null) {
            this.matriculaAluno = usuario.getAluno().getMatricula();
        }
        this.token = token;
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

    public String getToken() {
        return token;
    }
}
