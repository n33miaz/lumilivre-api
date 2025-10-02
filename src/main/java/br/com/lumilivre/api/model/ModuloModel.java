package br.com.lumilivre.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "modulo")
@Data 
public class ModuloModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String nome; // Ex: "1º Módulo", "3º Semestre", "2º Bimestre"
}