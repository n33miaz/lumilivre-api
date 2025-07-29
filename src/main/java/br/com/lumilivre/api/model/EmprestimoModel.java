package br.com.lumilivre.api.model;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
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
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "emprestimo")
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
    private AlunoModel aluno;

    @ManyToOne
    @JoinColumn(name = "exemplar_tombo", nullable = false) 
    private ExemplarModel exemplar;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LocalDateTime getDataEmprestimo() {
		return dataEmprestimo;
	}

	public void setDataEmprestimo(LocalDateTime dataEmprestimo) {
		this.dataEmprestimo = dataEmprestimo;
	}

	public LocalDateTime getDataDevolucao() {
		return dataDevolucao;
	}

	public void setDataDevolucao(LocalDateTime dataDevolucao) {
		this.dataDevolucao = dataDevolucao;
	}

	public Penalidade getPenalidade() {
		return penalidade;
	}

	public void setPenalidade(Penalidade penalidade) {
		this.penalidade = penalidade;
	}

	public StatusEmprestimo getStatusEmprestimo() {
		return statusEmprestimo;
	}

	public void setStatusEmprestimo(StatusEmprestimo statusEmprestimo) {
		this.statusEmprestimo = statusEmprestimo;
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

    


}
