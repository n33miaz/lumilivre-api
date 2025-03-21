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

	}
	


