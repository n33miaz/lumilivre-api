package br.com.lumilivre.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlterarSenhaRequest {

	private String matricula;
	private String senhaAtual;
	private String novaSenha;
}