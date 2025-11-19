package br.com.lumilivre.api.dto.aluno;

public record AlunoRankingResponse(String matricula, String nome, int emprestimosCount) {

	public String matricula() {
		return matricula;
	}

	public String nome() {
		return nome;
	}

	public int emprestimosCount() {
		return emprestimosCount;
	}

}
