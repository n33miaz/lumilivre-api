package br.com.lumilivre.api.dto.livro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExemplarRequest {

	private String tombo;
	private String status_livro;
	private Long livro_id;
	private String localizacao_fisica;
}