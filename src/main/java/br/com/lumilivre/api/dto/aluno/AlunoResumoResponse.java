package br.com.lumilivre.api.dto.aluno;

import java.time.LocalDate;
import br.com.lumilivre.api.enums.Penalidade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlunoResumoResponse {
	private Penalidade penalidade;
	private String matricula;
	private String cursoNome;
	private String nomeCompleto;
	private LocalDate dataNascimento;
	private String email;
	private String celular;
}