package br.com.lumilivre.api.model;

import java.sql.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "autor")
public class AutorModel {

	@Id
	@Column(name = "codigo", unique = true)
	private String codigo;

	@NotNull
	@Column(name = "nome", nullable = false, length = 55)
	private String nome;

	@Column(name = "pseudonimo", length = 55)
	private String pseudonimo;

	@JsonFormat(pattern = "dd/MM/yyyy")
	@Column(name = "data_nascimento")
	private Date data_nascimento;

	@JsonFormat(pattern = "dd/MM/yyyy")
	@Column(name = "data_falecimento")
	private Date data_falecimento;

	@Column(name = "genero_literario", length = 55)
	private String genero_literario;

	@Column(name = "nacionalidade", length = 55)
	private String nacionalidade;

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getPseudonimo() {
		return pseudonimo;
	}

	public void setPseudonimo(String pseudonimo) {
		this.pseudonimo = pseudonimo;
	}

	public Date getData_nascimento() {
		return data_nascimento;
	}

	public void setData_nascimento(Date data_nascimento) {
		this.data_nascimento = data_nascimento;
	}

	public Date getData_falecimento() {
		return data_falecimento;
	}

	public void setData_falecimento(Date data_falecimento) {
		this.data_falecimento = data_falecimento;
	}

	public String getGenero_literario() {
		return genero_literario;
	}

	public void setGenero_literario(String genero_literario) {
		this.genero_literario = genero_literario;
	}

	public String getNacionalidade() {
		return nacionalidade;
	}

	public void setNacionalidade(String nacionalidade) {
		this.nacionalidade = nacionalidade;
	}

}
