package br.com.lumilivre.api.dto.exemplar;

public class ExemplarRequest {

	private String tombo;
	private String status_livro;
	private Long livro_id;
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

	public String getStatus_livro() {
		return status_livro;
	}

	public void setStatus_livro(String status_livro) {
		this.status_livro = status_livro;
	}

	public Long getLivro_id() {
		return livro_id;
	}

	public void setLivro_id(Long livro_id) {
		this.livro_id = livro_id;
	}

}
