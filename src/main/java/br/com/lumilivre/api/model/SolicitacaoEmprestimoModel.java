package br.com.lumilivre.api.model;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitacao_emprestimo")
public class SolicitacaoEmprestimoModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "aluno_matricula", nullable = false)
	private AlunoModel aluno;

	@ManyToOne
	@JoinColumn(name = "exemplar_tombo", nullable = false)
	private ExemplarModel exemplar;

	@Column(name = "data_solicitacao", nullable = false)
	private LocalDateTime dataSolicitacao = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

	@Column(name = "observacao")
	private String observacao;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public AlunoModel getAluno() {
		return aluno;
	}

	public void setAluno(AlunoModel aluno) {
		this.aluno = aluno;
	}

	public ExemplarModel getExemplar() {
		return exemplar;
	}

	public void setExemplar(ExemplarModel exemplar) {
		this.exemplar = exemplar;
	}

	public LocalDateTime getDataSolicitacao() {
		return dataSolicitacao;
	}

	public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
		this.dataSolicitacao = dataSolicitacao;
	}

	public StatusSolicitacao getStatus() {
		return status;
	}

	public void setStatus(StatusSolicitacao status) {
		this.status = status;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

}
