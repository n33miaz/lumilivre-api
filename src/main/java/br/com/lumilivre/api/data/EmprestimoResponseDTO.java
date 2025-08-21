package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;

public class EmprestimoResponseDTO {

	private Integer id;
	private LocalDateTime dataEmprestimo;
	private LocalDateTime dataDevolucao;
	private StatusEmprestimo status;
	private Penalidade penalidade;
	private String livroTitulo;

	public EmprestimoResponseDTO(Integer id, LocalDateTime dataEmprestimo, LocalDateTime dataDevolucao,
			StatusEmprestimo status, Penalidade penalidade, String livroTitulo) {
		this.id = id;
		this.dataEmprestimo = dataEmprestimo;
		this.dataDevolucao = dataDevolucao;
		this.status = status;
		this.penalidade = penalidade;
		this.livroTitulo = livroTitulo;
	}

	// Getters e setters
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

	public StatusEmprestimo getStatus() {
		return status;
	}

	public void setStatus(StatusEmprestimo status) {
		this.status = status;
	}

	public Penalidade getPenalidade() {
		return penalidade;
	}

	public void setPenalidade(Penalidade penalidade) {
		this.penalidade = penalidade;
	}

	public String getLivroTitulo() {
		return livroTitulo;
	}

	public void setLivroTitulo(String livroTitulo) {
		this.livroTitulo = livroTitulo;
	}
}
