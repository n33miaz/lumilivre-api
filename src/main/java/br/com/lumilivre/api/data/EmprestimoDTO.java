package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class EmprestimoDTO {

	private Integer id;

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime data_emprestimo;

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime data_devolucao;

	private String penalidade;
	private String status_emprestimo;
	private String aluno_matricula;
	private String exemplar_tombo;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LocalDateTime getData_emprestimo() {
		return data_emprestimo;
	}

	public void setData_emprestimo(LocalDateTime data_emprestimo) {
		this.data_emprestimo = data_emprestimo;
	}

	public LocalDateTime getData_devolucao() {
		return data_devolucao;
	}

	public void setData_devolucao(LocalDateTime data_devolucao) {
		this.data_devolucao = data_devolucao;
	}

	public String getPenalidade() {
		return penalidade;
	}

	public void setPenalidade(String penalidade) {
		this.penalidade = penalidade;
	}

	public String getStatus_emprestimo() {
		return status_emprestimo;
	}

	public void setStatus_emprestimo(String status_emprestimo) {
		this.status_emprestimo = status_emprestimo;
	}

	public String getAluno_matricula() {
		return aluno_matricula;
	}

	public void setAluno_matricula(String aluno_matricula) {
		this.aluno_matricula = aluno_matricula;
	}

	public String getExemplar_tombo() {
		return exemplar_tombo;
	}

	public void setExemplar_tombo(String exemplar_tombo) {
		this.exemplar_tombo = exemplar_tombo;
	}

}
