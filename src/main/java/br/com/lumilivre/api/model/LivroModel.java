package br.com.lumilivre.api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "livro")
@Getter
@Setter


public class LivroModel {
    @Id    
    @Column(name = "Isbn")
    private String isbn;

    @Column(name = "Nome")
    private String nome;
    
    @Column(name = "Subtitulo")
    private String subtitulo;
    
    
    @Column(name = "Autor")
    private String autor;
    
    @Column(name = "Editora")
    private String editora;

    @Column(name = "Edicao")
    private String edicao;

    @Column(name = "AnoPublicacao")
    private LocalDate anoPublicacao;
    
    @Column(name = "NumeroPaginas")
    private int numeroPaginas;
    
    @Column(name = "Idioma")
    private String idioma;
    
    @Column(name = "Genero")
    private String genero;
    
    @Column(name = "Subgenero")
    private String subgenero;
    
    @Column(name = "ClassificacaoEtaria")
    private Integer classificacaoEtaria;
    
    @Column(name = "DetalhesPublicacao")
    private String detalhesPublicacao;
    
    @Column(name = "DimensoesLivro")
    private String dimensoesLivro;
    
    @Column(name = "TipoCapa")
    private String tipoCapa;
    
    @Column(name = "Colecao")
    private String colecao;
    
    @Column(name = "NumeroCapitulo")
    private int numeroCapitulo;
    
    @Column(name = "Sinopse")
    private String sinopse;
        
    @Column(name = "PalavrasChaves")
    private String palavrasChave;

    @Column(name = "Capa")
    private byte[] capa;

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getSubtitulo() {
		return subtitulo;
	}

	public void setSubtitulo(String subtitulo) {
		this.subtitulo = subtitulo;
	}

	public String getAutor() {
		return autor;
	}

	public void setAutor(String autor) {
		this.autor = autor;
	}

	public String getEditora() {
		return editora;
	}

	public void setEditora(String editora) {
		this.editora = editora;
	}

	public String getEdicao() {
		return edicao;
	}

	public void setEdicao(String edicao) {
		this.edicao = edicao;
	}

	public LocalDate getAnoPublicacao() {
		return anoPublicacao;
	}

	public void setAnoPublicacao(LocalDate anoPublicacao) {
		this.anoPublicacao = anoPublicacao;
	}

	public int getNumeroPaginas() {
		return numeroPaginas;
	}

	public void setNumeroPaginas(int numeroPaginas) {
		this.numeroPaginas = numeroPaginas;
	}

	public String getIdioma() {
		return idioma;
	}

	public void setIdioma(String idioma) {
		this.idioma = idioma;
	}

	public String getGenero() {
		return genero;
	}

	public void setGenero(String genero) {
		this.genero = genero;
	}

	public String getSubgenero() {
		return subgenero;
	}

	public void setSubgenero(String subgenero) {
		this.subgenero = subgenero;
	}

	public Integer getClassificacaoEtaria() {
		return classificacaoEtaria;
	}

	public void setClassificacaoEtaria(Integer classificacaoEtaria) {
		this.classificacaoEtaria = classificacaoEtaria;
	}

	public String getDetalhesPublicacao() {
		return detalhesPublicacao;
	}

	public void setDetalhesPublicacao(String detalhesPublicacao) {
		this.detalhesPublicacao = detalhesPublicacao;
	}

	public String getDimensoesLivro() {
		return dimensoesLivro;
	}

	public void setDimensoesLivro(String dimensoesLivro) {
		this.dimensoesLivro = dimensoesLivro;
	}

	public String getTipoCapa() {
		return tipoCapa;
	}

	public void setTipoCapa(String tipoCapa) {
		this.tipoCapa = tipoCapa;
	}

	public String getColecao() {
		return colecao;
	}

	public void setColecao(String colecao) {
		this.colecao = colecao;
	}

	public int getNumeroCapitulo() {
		return numeroCapitulo;
	}

	public void setNumeroCapitulo(int numeroCapitulo) {
		this.numeroCapitulo = numeroCapitulo;
	}

	public String getSinopse() {
		return sinopse;
	}

	public void setSinopse(String sinopse) {
		this.sinopse = sinopse;
	}

	public String getPalavrasChave() {
		return palavrasChave;
	}

	public void setPalavrasChave(String palavrasChave) {
		this.palavrasChave = palavrasChave;
	}

	public byte[] getCapa() {
		return capa;
	}

	public void setCapa(byte[] capa) {
		this.capa = capa;
	}
    
    
    

	}
	


