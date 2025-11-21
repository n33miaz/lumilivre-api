package br.com.lumilivre.api.dto.aluno;

public record AlunoRankingResponse(
		String matricula,
		String nome,
		int emprestimosCount) {
}