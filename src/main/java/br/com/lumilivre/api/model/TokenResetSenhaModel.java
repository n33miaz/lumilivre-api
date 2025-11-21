package br.com.lumilivre.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "token_reset_senha")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResetSenhaModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String token;

	@OneToOne(targetEntity = UsuarioModel.class, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false, name = "usuario_id")
	@ToString.Exclude
	private UsuarioModel usuario;

	@Column(nullable = false)
	private LocalDateTime dataExpiracao;

	public TokenResetSenhaModel(String token, UsuarioModel usuario, int minutosParaExpirar) {
		this.token = token;
		this.usuario = usuario;
		this.dataExpiracao = LocalDateTime.now().plusMinutes(minutosParaExpirar);
	}

	public boolean isExpirado() {
		return LocalDateTime.now().isAfter(this.dataExpiracao);
	}
}