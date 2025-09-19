package br.com.lumilivre.api.data;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusEmprestimo;

public class ListaEmprestimoDTO {
	private StatusEmprestimo statusEmprestimo;
	private String livroNome;
	private String livroTombo;
	private String nomeAluno;
	private String curso;
	private LocalDateTime dataEmprestimo;
	private LocalDateTime dataDevolucao;

	public ListaEmprestimoDTO(StatusEmprestimo statusEmprestimo, String livroNome, String livroTombo, String nomeAluno,
			String curso, LocalDateTime dataEmprestimo, LocalDateTime dataDevolucao) {
		this.statusEmprestimo = statusEmprestimo;
		this.livroNome = livroNome;
		this.livroTombo = livroTombo;
		this.nomeAluno = nomeAluno;
		this.curso = curso;
		this.dataEmprestimo = dataEmprestimo;
		this.dataDevolucao = dataDevolucao;
	}

	public StatusEmprestimo getStatusEmprestimo() {
		return statusEmprestimo;
	}

	public void setStatusEmprestimo(StatusEmprestimo statusEmprestimo) {
		this.statusEmprestimo = statusEmprestimo;
	}

	public String getLivroNome() {
		return livroNome;
	}

	public void setLivroNome(String livroNome) {
		this.livroNome = livroNome;
	}

	public String getLivroTombo() {
		return livroTombo;
	}

	public void setLivroTombo(String livroTombo) {
		this.livroTombo = livroTombo;
	}

	public String getNomeAluno() {
		return nomeAluno;
	}

	public void setNomeAluno(String nomeAluno) {
		this.nomeAluno = nomeAluno;
	}

	public String getCurso() {
		return curso;
	}

	public void setCurso(String curso) {
		this.curso = curso;
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

}
