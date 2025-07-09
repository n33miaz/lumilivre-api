package br.com.lumilivre.api.model;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "livro")
public class LivroModel {

    @Id
    @NotNull
    @Column(name = "isbn", length = 20, nullable = false)
    private String isbn;

    @NotNull
    @Column(name = "nome", length = 255, nullable = false)
    private String nome;

    @NotNull
    @Column(name = "data_lancamento", nullable = false)
    private LocalDate data_lancamento;

    @NotNull
    @Column(name = "numero_paginas", nullable = false)
    private Integer numero_paginas;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cdd", length = 10, nullable = false)
    private Cdd cdd;

    @NotNull
    @Column(name = "editora", length = 55, nullable = false)
    private String editora;

    @Column(name = "numero_capitulos")
    private Integer numero_capitulos;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_etaria", length = 55, nullable = false)
    private ClassificacaoEtaria classificacao_etaria;

    @Column(name = "edicao", length = 55)
    private String edicao;

    @Column(name = "volume")
    private Integer volume;

    @Column(name = "quantidade")
    private Integer quantidade;

    @Column(name = "sinopse", columnDefinition = "VARCHAR(MAX)")
    private String sinopse;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_capa", length = 55)
    private TipoCapa tipo_capa;

    @Column(name = "imagem", length = 500)
    private String imagem;

    @ManyToOne
    @JoinColumn(name = "genero_id")
    private GeneroModel genero;

    @ManyToOne
    @JoinColumn(name = "autor_codigo")
    private AutorModel autor;

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

    public LocalDate getData_lancamento() {
        return data_lancamento;
    }

    public void setData_lancamento(LocalDate data_lancamento) {
        this.data_lancamento = data_lancamento;
    }

    public Integer getNumero_paginas() {
        return numero_paginas;
    }

    public void setNumero_paginas(Integer numero_paginas) {
        this.numero_paginas = numero_paginas;
    }

    public Cdd getCdd() {
        return cdd;
    }

    public void setCdd(Cdd cdd) {
        this.cdd = cdd;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
    }

    public Integer getNumero_capitulos() {
        return numero_capitulos;
    }

    public void setNumero_capitulos(Integer numero_capitulos) {
        this.numero_capitulos = numero_capitulos;
    }

    public ClassificacaoEtaria getClassificacao_etaria() {
        return classificacao_etaria;
    }

    public void setClassificacao_etaria(ClassificacaoEtaria classificacao_etaria) {
        this.classificacao_etaria = classificacao_etaria;
    }

    public String getEdicao() {
        return edicao;
    }

    public void setEdicao(String edicao) {
        this.edicao = edicao;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getSinopse() {
        return sinopse;
    }

    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }

    public TipoCapa getTipo_capa() {
        return tipo_capa;
    }

    public void setTipo_capa(TipoCapa tipo_capa) {
        this.tipo_capa = tipo_capa;
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public GeneroModel getGenero() {
        return genero;
    }

    public void setGenero(GeneroModel genero) {
        this.genero = genero;
    }

    public AutorModel getAutor() {
        return autor;
    }

    public void setAutor(AutorModel autor) {
        this.autor = autor;
    }

    // Getters e setters podem ser gerados via Lombok ou IDE

    
}
