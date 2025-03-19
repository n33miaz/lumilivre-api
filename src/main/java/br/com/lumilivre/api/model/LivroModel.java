package br.com.lumilivre.api.model;

import java.time.LocalDate;
import jakarta.persistence.Entity;
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
    private String isbn;
    private String nome;
    private String subtitulo;
    private String autor;
    private String editora;
    private String edicao;
    private LocalDate anoPublicacao;
    private int numeroPaginas;
    private String idioma;
    private String genero;
    private String subgenero;
    private Integer classificacaoEtaria;
    private String detalhesPublicacao;
    private String dimensoesLivro;
    private String tipoCapa;
    private String colecao;
    private int numeroCapitulo;
    private String sinopse;
    private String palavrasChave;
    private byte[] capa;

}
