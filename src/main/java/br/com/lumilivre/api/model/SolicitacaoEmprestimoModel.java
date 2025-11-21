package br.com.lumilivre.api.model;

import java.time.LocalDateTime;
import br.com.lumilivre.api.enums.StatusSolicitacao;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "solicitacao_emprestimo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoEmprestimoModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "aluno_matricula", nullable = false)
	@ToString.Exclude
	private AlunoModel aluno;

	@ManyToOne
	@JoinColumn(name = "exemplar_tombo", nullable = false)
	@ToString.Exclude
	private ExemplarModel exemplar;

	@Column(name = "data_solicitacao", nullable = false)
	@Builder.Default
	private LocalDateTime dataSolicitacao = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	@Builder.Default
	private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

	@Column(name = "observacao")
	private String observacao;
}