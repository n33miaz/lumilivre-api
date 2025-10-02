package br.com.lumilivre.api.model;

import br.com.lumilivre.api.enums.Turno;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; 
import lombok.Setter; 

@Entity
@Table(name = "curso")
@Getter 
@Setter 
public class CursoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "Nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "Descricao", length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 55)
    private Turno turno;

    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private ModuloModel modulo;

    // todos os getters e setters manuais foram deletados
}