package br.com.lumilivre.api.model;

import br.com.lumilivre.api.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "usuario")
public class UsuarioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "senha", length = 255)
    private String senha;

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private Role role;


    public Integer getId() {
        return id;
    }


    public void setId(Integer id) {
        this.id = id;
    }


    public String getEmail() {
        return email;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public String getSenha() {
        return senha;
    }


    public void setSenha(String senha) {
        this.senha = senha;
    }


    public Role getRole() {
        return role;
    }


    public void setRole(Role role) {
        this.role = role;
    }

    

}
