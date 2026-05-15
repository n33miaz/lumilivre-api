package br.com.lumilivre.api.dto.v1.aluno;

public record AlunoRankingResponse(
		String matricula,
		String nome,
		long emprestimosCount) {
}
