package br.com.lumilivre.api.model;
import br.com.lumilivre.api.model.CursoModel;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table (name = "alunos")
public class AlunoModel {

    @Id
    @Column (name = "matricula", unique = true)
    private String matricula;

    @Column(name = "nome")
    private String nome;

    @Column(name = "sobrenome")
    private String sobrenome;

	@Column(name = "cpf")
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column (name = "celular")
    private String celular;

    @Column (name = "email")
    private String email;

    // Criar o relacionamento para o curso do aluno

    @OneToOne
    @JoinColumn(name =  "curso_id")
    private CursoModel curso;

	@OneToOne
    @JoinColumn(name =  "endereco_cep")
    private EnderecoModel endereco;

    

    // Criar a parte de Endereço, integrando API de CEP

    

}
