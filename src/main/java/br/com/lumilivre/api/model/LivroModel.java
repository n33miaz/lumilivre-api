package br.com.lumilivre.api.model;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "Livro")
public class LivroModel {

    @Id
    private String ISBN;

    @Size(min = 2, max = 255)
    @NotBlank(message = "O nome é obrigatório")
    @Column(name = "Nome", nullable = false, length = 255)    
    private String nome;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate data_lancamento;

    @Column(name = "numero_paginas", nullable = false)
    private int numero_paginas;

    @Column(name = "editora", nullable = false)
    private String editora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cdd cdd;

    @Column(name = "numero_capitulos")
    private String numero_capitulos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassificacaoEtaria classificacao_etaria;

    @Column(name = "edicao")
    private String edicao;

    @Column(name = "volume")
    private int volume;

    @Column(name = "quantidade")
    private int quantidade;

    @Column(name = "sinopse")
    private String sinopse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCapa tipo_capa;

    @Column(name = "imagem")
    private byte imagem;


    // relacionamento com genero
    // relacionamento com autor
}
