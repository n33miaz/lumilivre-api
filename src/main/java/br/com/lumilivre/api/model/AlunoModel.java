package br.com.lumilivre.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import br.com.lumilivre.api.enums.Penalidade;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "aluno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "matricula")
@ToString(exclude = { "usuario", "curso", "turno", "modulo" })
public class AlunoModel {

	@Id
	@Column(name = "matricula", length = 5, unique = true)
	private String matricula;

	@NotNull
	@Column(name = "nome_completo", nullable = false, length = 110)
	private String nomeCompleto;

	@Column(name = "cpf", nullable = false, length = 11, unique = true)
	private String cpf;

	@JsonFormat(pattern = "dd/MM/yyyy")
	@Column(name = "data_nascimento")
	private LocalDate dataNascimento;

	@NotNull
	@Column(name = "celular", nullable = false, length = 11)
	private String celular;

	@NotNull
	@Column(name = "email", length = 255, unique = true)
	private String email;

	@ManyToOne
	@JoinColumn(name = "curso_id", nullable = false)
	private CursoModel curso;

	@ManyToOne
	@JoinColumn(name = "turno_id")
	private TurnoModel turno;

	@ManyToOne
	@JoinColumn(name = "modulo_id")
	private ModuloModel modulo;

	@JsonManagedReference
	@OneToOne(mappedBy = "aluno", cascade = CascadeType.ALL, orphanRemoval = true)
	private UsuarioModel usuario;

	@Size(min = 8, max = 8, message = "CEP deve ter exatamente 8 caracteres")
	@Column(name = "cep", length = 8)
	private String cep;

	@Column(name = "logradouro", length = 255)
	private String logradouro;

	@Column(name = "complemento", length = 55)
	private String complemento;

	@Column(name = "bairro", length = 255)
	private String bairro;

	@Column(name = "localidade", length = 255)
	private String localidade;

	@Column(name = "uf", length = 2)
	private String uf;

	@Column(name = "numero_casa")
	private Integer numero_casa;

	@Enumerated(EnumType.STRING)
	@Column(name = "penalidade", length = 55)
	private Penalidade penalidade;

	private LocalDateTime penalidadeExpiraEm;

	@Column(name = "emprestimos_count", nullable = false)
	private Integer emprestimosCount = 0;

	public int getEmprestimosCount() {
		return emprestimosCount != null ? emprestimosCount : 0;
	}

	public void incrementarEmprestimos() {
		this.emprestimosCount = getEmprestimosCount() + 1;
	}

	public void decrementarEmprestimos() {
		if (getEmprestimosCount() > 0) {
			this.emprestimosCount--;
		}
	}
}