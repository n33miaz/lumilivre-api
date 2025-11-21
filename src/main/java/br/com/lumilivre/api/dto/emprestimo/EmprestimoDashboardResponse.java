package br.com.lumilivre.api.dto.emprestimo;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoDashboardResponse {

	private String livroNome;
	private String alunoNome;
	private LocalDateTime dataDevolucao;
	private StatusEmprestimo statusEmprestimo;
}