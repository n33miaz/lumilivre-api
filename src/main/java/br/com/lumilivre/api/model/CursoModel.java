package br.com.lumilivre.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;



@Entity
@Table(name = "curso")
@Getter
@Setter

public class CursoModel {    

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "NomeCurso") 
    private String nome;

    @Column(name = "Descricao")
    private String descricao;

    @Column(name = "Turno")
    private String turno;

	@Column(name = "Modulo")
	private String modulo;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    
 
	
}