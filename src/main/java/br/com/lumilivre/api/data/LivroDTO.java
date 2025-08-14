package br.com.lumilivre.api.data;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;


public class LivroDTO  {

    private String isbn;
    private String nome;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate data_lancamento;
    private Integer numero_paginas;
    private String cdd;
    private String editora;
    private Integer numero_capitulos;
    private String classificacao_etaria;
    private String edicao;
    private Integer volume;
    private Integer quantidade;
    private String sinopse;
    private String tipo_capa;
    private String imagem;
    private String genero;
    private String autor;

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

    public String getCdd() {
        return cdd;
    }

    public void setCdd(String cdd) {
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

    public String getClassificacao_etaria() {
        return classificacao_etaria;
    }

    public void setClassificacao_etaria(String classificacao_etaria) {
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

    public String getTipo_capa() {
        return tipo_capa;
    }

    public void setTipo_capa(String tipo_capa) {
        this.tipo_capa = tipo_capa;
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }
}