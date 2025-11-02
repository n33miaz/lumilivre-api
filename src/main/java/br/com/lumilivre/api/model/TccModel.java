package br.com.lumilivre.api.model;

import jakarta.persistence.*;


@Entity
@Table(name = "tcc")
public class TccModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String alunos;
    private String orientadores;
    private String curso;
    private String ano_conclusao;
    private String semestre_conclusao;
    private String arquivo_pdf;
    private String link_externo;

    private Boolean ativo = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getAlunos() {
		return alunos;
	}

	public void setAlunos(String alunos) {
		this.alunos = alunos;
	}

	public String getOrientadores() {
		return orientadores;
	}

	public void setOrientadores(String orientadores) {
		this.orientadores = orientadores;
	}

	public String getCurso() {
		return curso;
	}

	public void setCurso(String curso) {
		this.curso = curso;
	}

	public String getAno_conclusao() {
		return ano_conclusao;
	}

	public void setAno_conclusao(String ano_conclusao) {
		this.ano_conclusao = ano_conclusao;
	}

	public String getSemestre_conclusao() {
		return semestre_conclusao;
	}

	public void setSemestre_conclusao(String semestre_conclusao) {
		this.semestre_conclusao = semestre_conclusao;
	}

	public String getArquivo_pdf() {
		return arquivo_pdf;
	}

	public void setArquivo_pdf(String arquivo_pdf) {
		this.arquivo_pdf = arquivo_pdf;
	}

	public String getLink_externo() {
		return link_externo;
	}

	public void setLink_externo(String link_externo) {
		this.link_externo = link_externo;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}
    
    
}
