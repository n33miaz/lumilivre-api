package br.com.lumilivre.api.data;

import br.com.lumilivre.api.enums.Penalidade;

public class ListaAlunoDTO {
	private Penalidade penalidade;
	private String matricula;
	private String nome;
	private String email;
	private String celular;
	private String cursoNome;

	public ListaAlunoDTO(Penalidade penalidade, String matricula, String nome, String email, String celular,
			String cursoNome) {
		this.penalidade = penalidade;
		this.matricula = matricula;
		this.nome = nome;
		this.email = email;
		this.celular = celular;
		this.cursoNome = cursoNome;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getMatricula() {
		return matricula;
	}

	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}

	public String getCursoNome() {
		return cursoNome;
	}

	public void setCursoNome(String cursoNome) {
		this.cursoNome = cursoNome;
	}

	public Penalidade getPenalidade() {
		return penalidade;
	}

	public void setPenalidade(Penalidade penalidade) {
		this.penalidade = penalidade;
	}

	public String getCelular() {
		return celular;
	}

	public void setCelular(String celular) {
		this.celular = celular;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
