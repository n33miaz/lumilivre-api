package br.com.lumilivre.api.dto.v1.emprestimo;

import java.time.LocalDate;
import java.util.UUID;

import br.com.lumilivre.api.enums.LoanStatus;

public record EmprestimoAtivoResponse(
		UUID id,
		String livroNome,
		String alunoNome,
		String alunoMatricula,
		String tombo,
		LocalDate dataEmprestimo,
		LocalDate dataDevolucao,
		LoanStatus statusEmprestimo) {
}
