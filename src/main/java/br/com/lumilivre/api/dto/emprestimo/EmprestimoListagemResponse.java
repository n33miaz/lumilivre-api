package br.com.lumilivre.api.dto.emprestimo;

import java.time.LocalDateTime;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoListagemResponse {

	private Integer id;
	private StatusEmprestimo statusEmprestimo;
	private String livroNome;
	private String livroTombo;
	private String nomeAluno;
	private String matriculaAluno;
	private String curso;
	private LocalDateTime dataEmprestimo;
	private LocalDateTime dataDevolucao;
}