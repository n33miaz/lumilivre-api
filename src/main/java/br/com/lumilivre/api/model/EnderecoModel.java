package br.com.lumilivre.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.OneToOne;

@Entity
public class EnderecoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "logradouro")
    private String logradouro;


    @OneToOne(mappedBy = "endereco")  // A relação bidirecional
    private AlunoModel aluno;  // Relacionamento com a classe Aluno

    // Getters e Setters
}
