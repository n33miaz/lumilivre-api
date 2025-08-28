package br.com.lumilivre.api.data;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ListaAlunoDTO {
    private String nome;
    private String matricula;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate data_nascimento;
    private String cursoNome;
    
	public ListaAlunoDTO(String nome, String matricula, LocalDate data_nascimento, String cursoNome) {
		this.nome = nome;
		this.matricula = matricula;
		this.data_nascimento = data_nascimento;
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

	public LocalDate getData_nascimento() {
		return data_nascimento;
	}

	public void setData_nascimento(LocalDate data_nascimento) {
		this.data_nascimento = data_nascimento;
	}

	public String getCursoNome() {
		return cursoNome;
	}

	public void setCursoNome(String cursoNome) {
		this.cursoNome = cursoNome;
	}

	



}
