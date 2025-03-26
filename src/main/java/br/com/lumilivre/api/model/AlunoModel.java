package br.com.lumilivre.api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;

import jakarta.persistence.Id;

public class AlunoModel {
    

    @Id
    @Column (name = "Rm")
    private String matricula;

    @Column(name = "NomeAluno")
    private String nome;

	@Column(name = "Cpf")
    private String cpf;

    @Column(name = "DataNascimento")
    private LocalDate dataNascimento;

    @Column (name = "Telefone")
    private String telefone;

    @Column (name = "Nacionalidade")
    private String nacionalidade;

    @Column (name = "Genero")
    private String genero;

	public String getRm() {
		return matricula;
	}

	public void setRm(String matricula) {
		this.matricula = matricula;
	}

	public String getNome() {
		return nome;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getNacionalidade() {
		return nacionalidade;
	}

	public void setNacionalidade(String nacionalidade) {
		this.nacionalidade = nacionalidade;
	}

	public String getGenero() {
		return genero;
	}

	public void setGenero(String genero) {
		this.genero = genero;
	}

    

    // Criar o relacionamento para o curso do aluno
    // Criar a parte de Endereço, integrando API de CEP

    

}
