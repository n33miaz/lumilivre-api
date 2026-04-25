package br.com.lumilivre.api.dto.emprestimo;

import java.time.OffsetDateTime;
import java.util.UUID;

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

	private UUID id;

	@NotNull(message = "A data de empréstimo é obrigatória")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ssXXX")
	private OffsetDateTime data_emprestimo;

	@NotNull(message = "A data de devolução é obrigatória")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ssXXX")
	private OffsetDateTime data_devolucao;

	private String penalidade;
	private String status_emprestimo;

	@NotBlank(message = "A matrícula do aluno é obrigatória")
	private String aluno_matricula;

	@NotBlank(message = "O tombo do exemplar é obrigatório")
	private String exemplar_tombo;
}
