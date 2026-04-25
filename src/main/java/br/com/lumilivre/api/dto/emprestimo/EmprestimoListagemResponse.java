package br.com.lumilivre.api.dto.emprestimo;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoListagemResponse {

	private UUID id;
	private LoanStatus statusEmprestimo;
	private String livroNome;
	private String livroTombo;
	private String nomeAluno;
	private String matriculaAluno;
	private String curso;
	private OffsetDateTime dataEmprestimo;
	private OffsetDateTime dataDevolucao;
}
