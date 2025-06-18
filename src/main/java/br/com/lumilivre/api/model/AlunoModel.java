package br.com.lumilivre.api.model;

import br.com.lumilivre.api.model.CursoModel;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

@Entity
@Table(name = "aluno")
public class AlunoModel {

	@Id
	@Column(name = "matricula", length = 5, unique = true)
	private String matricula;

	@Size(min = 2, max = 55)
	@Column(name = "nome", nullable = false, length = 55)
	private String nome;

	@Size(min = 2, max = 11)
	@Column(name = "sobrenome", nullable = false)
	private String sobrenome;

	@Size(min = 11, max = 11)
	@Column(name = "cpf", nullable = false)
	private String cpf;

	@JsonFormat(pattern = "dd/MM/yyyy")
	@Column(name = "data_nascimento")
	private LocalDate dataNascimento;

	@Size(min = 11, max = 11)
	@Column(name = "celular", nullable = false)
	private String celular;

	@Size(min = 2, max = 255)
	@Column(name = "email")
	private String email;

	@ManyToOne
	@JoinColumn(name = "curso_id", nullable = false)
	private CursoModel curso;

	@Size(min = 8, max = 8, message = "CEP deve ter exatamente 8 caracteres")
	@Column(name = "cep")
	private String cep;

	@Column(name = "logradouro")
	private String logradouro;

	@Column(name = "complemento")
	private String complemento;

	@Column(name = "bairro")
	private String bairro;

	@Column(name = "localidade")
	private String localidade;

	@Column(name = "uf")
	private String uf;

	@Column(name = "numero_casa")
	private Integer numero_casa;

	@Column(name = "estado")
	private String estado;

	public String getMatricula() {
		return matricula;
	}

	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getSobrenome() {
		return sobrenome;
	}

	public void setSobrenome(String sobrenome) {
		this.sobrenome = sobrenome;
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

	public CursoModel getCurso() {
		return curso;
	}

	public void setCurso(CursoModel curso) {
		this.curso = curso;
	}

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public void setLogradouro(String logradouro) {
		this.logradouro = logradouro;
	}

	public String getComplemento() {
		return complemento;
	}

	public void setComplemento(String complemento) {
		this.complemento = complemento;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getLocalidade() {
		return localidade;
	}

	public void setLocalidade(String localidade) {
		this.localidade = localidade;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public Integer getNumero_casa() {
		return numero_casa;
	}

	public void setNumero_casa(Integer numero_casa) {
		this.numero_casa = numero_casa;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

}
