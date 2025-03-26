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
    private long codigo;

    @Column(name = "NomeCurso") 
    private String nome;

    @Column(name = "Descricao")
    private String descricao;

    @Column(name = "Turno")
    private String turno;

	public long getCodigo() {
		return codigo;
	}

	public void setCodigo(long codigo) {
		this.codigo = codigo;
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
    
    
}