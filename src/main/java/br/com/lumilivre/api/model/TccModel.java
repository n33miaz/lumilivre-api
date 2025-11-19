package br.com.lumilivre.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tcc")
@Getter
@Setter
public class TccModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String alunos;

    private String orientadores;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoModel curso;

    @Column(name = "ano_conclusao")
    private String anoConclusao;

    @Column(name = "semestre_conclusao")
    private String semestreConclusao;

    @Column(name = "arquivo_pdf")
    private String arquivoPdf;

    @Column(name = "link_externo")
    private String linkExterno;

    private Boolean ativo = true;

}