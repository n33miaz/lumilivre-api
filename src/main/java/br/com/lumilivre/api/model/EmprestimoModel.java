package br.com.lumilivre.api.model;

import java.time.LocalDateTime;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "emprestimo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotNull
	@Column(name = "data_emprestimo", nullable = false)
	private LocalDateTime dataEmprestimo;

	@NotNull
	@Column(name = "data_devolucao", nullable = false)
	private LocalDateTime dataDevolucao;

	@Enumerated(EnumType.STRING)
	@Column(name = "penalidade", length = 59)
	private Penalidade penalidade;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_emprestimo", length = 55)
	private StatusEmprestimo statusEmprestimo;

	@ManyToOne
	@JoinColumn(name = "aluno_matricula", nullable = false)
	@ToString.Exclude
	private Student aluno;

	@ManyToOne
	@JoinColumn(name = "exemplar_tombo", nullable = false)
	@ToString.Exclude
	private ExemplarModel exemplar;

	@Column(name = "renovacoes", nullable = false)
	private int renovacoes = 0;

	@Formula("(CASE WHEN status_emprestimo = 'COMPLETED' THEN 1 ELSE 0 END)")
	private int ordemStatus;
}
