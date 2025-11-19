package br.com.lumilivre.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curso")
@Getter
@Setter
public class CursoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 255, unique = true)
    private String nome;

    @OneToMany(mappedBy = "curso", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AlunoModel> alunos = new ArrayList<>();

}