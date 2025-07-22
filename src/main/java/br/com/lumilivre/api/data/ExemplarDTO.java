package br.com.lumilivre.api.data;

public class ExemplarDTO {
	
	private String tombo;
	private String status_livro;
	private String livro_isbn;
	
	
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
	public String getLivro_isbn() {
		return livro_isbn;
	}
	public void setLivro_isbn(String livro_isbn) {
		this.livro_isbn = livro_isbn;
	}
	
	
}
