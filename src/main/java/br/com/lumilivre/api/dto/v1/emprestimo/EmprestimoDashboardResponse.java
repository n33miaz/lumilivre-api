package br.com.lumilivre.api.dto.v1.emprestimo;

import java.time.OffsetDateTime;

import br.com.lumilivre.api.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoDashboardResponse {

	private String livroNome;
	private String alunoNome;
	private OffsetDateTime dataDevolucao;
	private LoanStatus statusEmprestimo;
}
