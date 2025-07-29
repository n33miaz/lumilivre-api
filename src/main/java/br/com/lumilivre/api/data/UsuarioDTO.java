package br.com.lumilivre.api.data;

import jakarta.validation.constraints.NotBlank;

public class UsuarioDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String senha;
    
    private String matriculaAluno;


	public String getMatriculaAluno() {
		return matriculaAluno;
	}

	public void setMatriculaAluno(String matriculaAluno) {
		this.matriculaAluno = matriculaAluno;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}	
    
    
}
