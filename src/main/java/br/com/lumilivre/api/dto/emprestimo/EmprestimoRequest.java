package br.com.lumilivre.api.dto.emprestimo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmprestimoRequest {

	private Integer id;

	@NotNull(message = "A data de empréstimo é obrigatória")
	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime data_emprestimo;

	@NotNull(message = "A data de devolução é obrigatória")
	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime data_devolucao;

	private String penalidade;
	private String status_emprestimo;

	@NotBlank(message = "A matrícula do aluno é obrigatória")
	private String aluno_matricula;

	@NotBlank(message = "O tombo do exemplar é obrigatório")
	private String exemplar_tombo;
}