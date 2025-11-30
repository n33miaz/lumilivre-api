package br.com.lumilivre.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Entity
@Table(name = "livro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivroModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isbn", length = 20, unique = true)
    private String isbn;

    @NotNull
    @Column(name = "nome", length = 255, nullable = false)
    private String nome;

    @Column(name = "data_lancamento", nullable = true)
    private LocalDate data_lancamento;

    @Column(name = "numero_paginas", nullable = true)
    private Integer numero_paginas;

    @ManyToOne
    @JoinColumn(name = "cdd_codigo")
    @ToString.Exclude
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

    @Column(name = "data_inclusao", nullable = false)
    private LocalDateTime dataInclusao;

    @OneToMany(mappedBy = "livro", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ExemplarModel> exemplares;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "livro_genero", joinColumns = @JoinColumn(name = "livro_id"), inverseJoinColumns = @JoinColumn(name = "genero_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<GeneroModel> generos = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.dataInclusao == null) {
            this.dataInclusao = LocalDateTime.now();
        }
    }
}