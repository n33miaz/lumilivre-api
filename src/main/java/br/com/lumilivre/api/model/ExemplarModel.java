package br.com.lumilivre.api.model;

import br.com.lumilivre.api.enums.StatusLivro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exemplar")
public class ExemplarModel {

	@Id
	@Column(name = "tombo", length = 10, unique = true)
	private String tombo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_livro", length = 55)
	private StatusLivro status_livro;

	@ManyToOne
	@JoinColumn(name = "livro_isbn", nullable = false)
	private LivroModel livro;

	@Column(name = "localizacao_fisica", nullable = false)
	private String localizacao_fisica;

	public String getLocalizacao_fisica() {
		return localizacao_fisica;
	}

	public void setLocalizacao_fisica(String localizacao_fisica) {
		this.localizacao_fisica = localizacao_fisica;
	}

	public String getTombo() {
		return tombo;
	}

	public void setTombo(String tombo) {
		this.tombo = tombo;
	}

	public StatusLivro getStatus_livro() {
		return status_livro;
	}

	public void setStatus_livro(StatusLivro status_livro) {
		this.status_livro = status_livro;
	}

	public LivroModel getLivro_isbn() {
		return livro;
	}

	public void setLivro_isbn(LivroModel livro_isbn) {
		this.livro = livro_isbn;
	}

	
}
