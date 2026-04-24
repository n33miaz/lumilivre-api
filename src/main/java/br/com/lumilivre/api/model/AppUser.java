package br.com.lumilivre.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import br.com.lumilivre.api.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "AppUser")
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "senha", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private Role role;

    @OneToOne
    @JsonBackReference
    @JoinColumn(name = "aluno_matricula", referencedColumnName = "matricula")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Student student;

    public String getSenha() {
        return passwordHash;
    }

    public void setSenha(String senha) {
        this.passwordHash = senha;
    }

    public Student getAluno() {
        return student;
    }

    public void setAluno(Student aluno) {
        this.student = aluno;
    }
}
