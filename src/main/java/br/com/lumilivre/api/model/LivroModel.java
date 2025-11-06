package br.com.lumilivre.api.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;

import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "livro")
public class LivroModel {

    @Id
    @GeneratedValue(generator = "custom-generator")
    @GenericGenerator(name = "custom-generator", strategy = "br.com.lumilivre.api.utils.AssignedIdentityGenerator")
    private Long id;

    @Column(name = "isbn", length = 20, unique = true)
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
    @ManyToOne
    @JoinColumn(name = "cdd_codigo")
    private CddModel cdd;

    @NotNull
    @Column(name = "editora", length = 55, nullable = false)
    private String editora;

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

    @Column(name = "sinopse", columnDefinition = "TEXT")
    private String sinopse;

    @Column(name = "autor", length = 255)
    private String autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_capa", length = 55)
    private TipoCapa tipo_capa;

    @Column(name = "imagem", length = 5000)
    private String imagem;

    @OneToMany(mappedBy = "livro", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ExemplarModel> exemplares;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "livro_genero", joinColumns = @JoinColumn(name = "livro_id"), inverseJoinColumns = @JoinColumn(name = "genero_id"))
    private Set<GeneroModel> generos = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public CddModel getCdd() {
        return cdd;
    }

    public void setCdd(CddModel cdd) {
        this.cdd = cdd;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
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

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public Set<GeneroModel> getGeneros() {
        return generos;
    }

    public void setGeneros(Set<GeneroModel> generos) {
        this.generos = generos;
    }

    public List<ExemplarModel> getExemplares() {
        return exemplares;
    }

    public void setExemplares(List<ExemplarModel> exemplares) {
        this.exemplares = exemplares;
    }
}
