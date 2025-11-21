package br.com.lumilivre.api.dto.livro;

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
public class ExemplarRequest {

	@NotBlank(message = "O tombo do exemplar é obrigatório")
	private String tombo;

	private String status_livro;

	@NotNull(message = "O ID do livro é obrigatório")
	private Long livro_id;

	@NotBlank(message = "A localização física é obrigatória")
	private String localizacao_fisica;
}