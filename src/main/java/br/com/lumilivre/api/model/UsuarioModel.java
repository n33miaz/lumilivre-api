package br.com.lumilivre.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import br.com.lumilivre.api.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

	@OneToOne
	@JsonBackReference
	@JoinColumn(name = "aluno_matricula", referencedColumnName = "matricula")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private AlunoModel aluno;
}