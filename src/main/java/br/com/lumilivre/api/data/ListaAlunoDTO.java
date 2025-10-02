package br.com.lumilivre.api.data;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.Penalidade;

public class ListaAlunoDTO {
	private Penalidade penalidade;
	private String matricula;
	private String cursoNome;
	private String nome;
	private String sobrenome;
	private LocalDate dataNascimento;
	private String email;
	private String celular; // contato

	public ListaAlunoDTO(Penalidade penalidade, String matricula, String cursoNome, String nome, String sobrenome, LocalDate dataNascimento, String email, String celular) {
		this.penalidade = penalidade;
		this.matricula = matricula;
		this.cursoNome = cursoNome;
		this.nome = nome;
		this.sobrenome = sobrenome;
        this.dataNascimento = dataNascimento;
		this.email = email;
		this.celular = celular;
	}

	public Penalidade getPenalidade() { return penalidade; }
	public void setPenalidade(Penalidade penalidade) { this.penalidade = penalidade; }

	public String getMatricula() { return matricula; }
	public void setMatricula(String matricula) { this.matricula = matricula; }

	public String getCursoNome() { return cursoNome; }
	public void setCursoNome(String cursoNome) { this.cursoNome = cursoNome; }

	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }

	public String getSobrenome() { return sobrenome; }
	public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }

	public LocalDate getDataNascimento() { return dataNascimento; }
	public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getCelular() { return celular; }
	public void setCelular(String celular) { this.celular = celular; }
}
