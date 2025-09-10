package br.com.lumilivre.api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.Getter;   
import lombok.Setter;   

@Entity
@Table(name = "token_reset_senha")

@Getter 
@Setter 

public class TokenResetSenhaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = UsuarioModel.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "usuario_id")
    private UsuarioModel usuario;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    public TokenResetSenhaModel() {}

    public TokenResetSenhaModel(String token, UsuarioModel usuario, int minutosParaExpirar) {
        this.token = token;
        this.usuario = usuario;
        this.dataExpiracao = LocalDateTime.now().plusMinutes(minutosParaExpirar);
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.dataExpiracao);
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public UsuarioModel getUsuario() {
		return usuario;
	}

	public void setUsuario(UsuarioModel usuario) {
		this.usuario = usuario;
	}

	public LocalDateTime getDataExpiracao() {
		return dataExpiracao;
	}

	public void setDataExpiracao(LocalDateTime dataExpiracao) {
		this.dataExpiracao = dataExpiracao;
	}
    
    
}