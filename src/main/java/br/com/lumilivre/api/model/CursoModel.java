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
    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 55)
    private Turno turno;

    @Column(name = "modulo", length = 55)
    private String modulo;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Turno getTurno() {
		return turno;
	}

	public void setTurno(Turno turno) {
		this.turno = turno;
	}

	public String getModulo() {
		return modulo;
	}

	public void setModulo(String modulo) {
		this.modulo = modulo;
	}
    
    
}