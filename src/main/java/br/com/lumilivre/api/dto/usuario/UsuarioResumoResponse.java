package br.com.lumilivre.api.dto.usuario;

import br.com.lumilivre.api.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResumoResponse {

	private Integer id;
	private String email;
	private Role role;
}