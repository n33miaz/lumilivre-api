package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusEmprestimo;

public class ListaEmprestimoDashboardDTO {
	private String livroNome;
	private String alunoNome;
	private LocalDateTime dataDevolucao;
	private StatusEmprestimo statusEmprestimo;

	public ListaEmprestimoDashboardDTO(String livroNome, String alunoNome, LocalDateTime dataDevolucao,
			StatusEmprestimo statusEmprestimo) {
		this.livroNome = livroNome;
		this.alunoNome = alunoNome;
		this.dataDevolucao = dataDevolucao;
		this.statusEmprestimo = statusEmprestimo;
	}

	public String getLivroNome() {
		return livroNome;
	}

	public void setLivroNome(String livroNome) {
		this.livroNome = livroNome;
	}

	public String getAlunoNome() {
		return alunoNome;
	}

	public void setAlunoNome(String alunoNome) {
		this.alunoNome = alunoNome;
	}

	public LocalDateTime getDataDevolucao() {
		return dataDevolucao;
	}

	public void setDataDevolucao(LocalDateTime dataDevolucao) {
		this.dataDevolucao = dataDevolucao;
	}

	public StatusEmprestimo getStatusEmprestimo() {
		return statusEmprestimo;
	}

	public void setStatusEmprestimo(StatusEmprestimo statusEmprestimo) {
		this.statusEmprestimo = statusEmprestimo;
	}

}
