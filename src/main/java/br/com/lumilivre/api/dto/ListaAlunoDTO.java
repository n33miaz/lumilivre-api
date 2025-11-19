package br.com.lumilivre.api.dto;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.Penalidade;

public class ListaAlunoDTO {
	private Penalidade penalidade;
	private String matricula;
	private String cursoNome;
	private String nomeCompleto;
	private LocalDate dataNascimento;
	private String email;
	private String celular; // contato

	public ListaAlunoDTO(Penalidade penalidade, String matricula, String cursoNome, String nomeCompleto,
			LocalDate dataNascimento, String email, String celular) {
		this.penalidade = penalidade;
		this.matricula = matricula;
		this.cursoNome = cursoNome;
		this.nomeCompleto = nomeCompleto;
		this.dataNascimento = dataNascimento;
		this.email = email;
		this.celular = celular;
	}

	public Penalidade getPenalidade() {
		return penalidade;
	}

	public void setPenalidade(Penalidade penalidade) {
		this.penalidade = penalidade;
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

	public String getNome() {
		return nomeCompleto;
	}

	public void setNome(String nomeCompleto) {
		this.nomeCompleto = nomeCompleto;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCelular() {
		return celular;
	}

	public void setCelular(String celular) {
		this.celular = celular;
	}
}
