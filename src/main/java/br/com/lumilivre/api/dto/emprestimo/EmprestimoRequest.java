package br.com.lumilivre.api.dto.emprestimo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime data_emprestimo;

	@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
	private LocalDateTime data_devolucao;

	private String penalidade;
	private String status_emprestimo;
	private String aluno_matricula;
	private String exemplar_tombo;
}